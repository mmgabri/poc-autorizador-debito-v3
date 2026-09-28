package br.com.mmgabri.config;

import br.com.mmgabri.adapters.redis.LedgerCompletionRedisPublisher;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    // Only publishes (sendMessage/getQueueUrl) - no long-poll, so a short socketTimeout
    // is safe here (unlike conta, which calls receiveMessage with wait-time-seconds).
    // Explicit tuning anyway, so we do not depend on the SDK defaults
    // (maxConnections=50) under high TPS spread across the replicas.
    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region:us-east-1}") String awsRegion) {
        var httpClient = ApacheHttpClient.builder()
                .maxConnections(200)
                .connectionTimeout(Duration.ofSeconds(2))
                .socketTimeout(Duration.ofSeconds(5))
                .connectionAcquisitionTimeout(Duration.ofSeconds(1))
                .build();

        return SqsClient.builder()
                .region(Region.of(awsRegion))
                .httpClient(httpClient)
                .build();
    }

    @Bean
    public BindableService protoReflectionService() {
        return ProtoReflectionService.newInstance();
    }

    // Boot 4.1 auto-configures a Jackson 3 ObjectMapper (tools.jackson.databind);
    // this service uses classic Jackson 2 (com.fasterxml.jackson.databind) directly
    // (SQS publisher/pub-sub), so it needs the explicit bean.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public GrpcServerExecutorProvider grpcServerExecutorProvider() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }

    // Identifies this replica to route the conta callback back to it through its
    // own Redis pub/sub channel (efetivacao:conta:{instanceId}) - the gRPC callback
    // may land on any replica through the k8s Service load balancing, not
    // necessarily on the one that dispatched it.
    @Bean
    public String instanceId() {
        return UUID.randomUUID().toString();
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            LedgerCompletionRedisSubscriber subscriber,
            String instanceId) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Without this, the container creates its own SimpleAsyncTaskExecutor (regular
        // platform thread) for the subscription and to dispatch onMessage - out of line
        // with the rest of the service, which uses virtual threads everywhere (gRPC, SQS).
        container.setSubscriptionExecutor(Executors.newVirtualThreadPerTaskExecutor());
        container.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
        container.addMessageListener(subscriber, new ChannelTopic(LedgerCompletionRedisPublisher.CHANNEL_PREFIX + instanceId));
        return container;
    }
}
