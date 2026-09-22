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

    // Só publica (sendMessage/getQueueUrl) - sem long-poll, então socketTimeout
    // curto é seguro aqui (diferente do conta, que faz receiveMessage com
    // wait-time-seconds). Tuning explícito mesmo assim, pra não depender dos
    // defaults do SDK (maxConnections=50) sob TPS alto distribuído nas réplicas.
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

    // Boot 4.1 auto-configura um ObjectMapper do Jackson 3 (tools.jackson.databind);
    // este serviço usa Jackson 2 clássico (com.fasterxml.jackson.databind) diretamente
    // (SQS publisher/pub-sub), então precisa do bean explícito.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public GrpcServerExecutorProvider grpcServerExecutorProvider() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }

    // Identifica esta réplica pra rotear o callback do conta de volta pra ela,
    // via canal Redis pub/sub próprio (efetivacao:conta:{instanceId}) - o
    // callback gRPC pode cair em qualquer réplica via load balancing do
    // Service do k8s, não necessariamente na que despachou.
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
        // Sem isso, o container cria seu próprio SimpleAsyncTaskExecutor (thread de
        // plataforma comum) pra assinatura e pra despachar onMessage - destoa do
        // resto do serviço, que usa virtual threads em tudo (gRPC, SQS).
        container.setSubscriptionExecutor(Executors.newVirtualThreadPerTaskExecutor());
        container.setTaskExecutor(Executors.newVirtualThreadPerTaskExecutor());
        container.addMessageListener(subscriber, new ChannelTopic(LedgerCompletionRedisPublisher.CHANNEL_PREFIX + instanceId));
        return container;
    }
}
