package br.com.mmgabri.config;

import br.com.mmgabri.adapters.grpc.server.LedgerControllerGrpc;
import br.com.mmgabri.adapters.grpc.server.RetornoContaControllerGrpc;
import br.com.mmgabri.adapters.redis.RetornoContaRedisPublisher;
import br.com.mmgabri.adapters.redis.RetornoContaRedisSubscriber;
import br.com.mmgabri.services.LedgerEfetivacaoService;
import br.com.mmgabri.services.LedgerSimulacaoService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.UUID;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean(name = "instanceId")
    public String instanceId() {
        return UUID.randomUUID().toString();
    }

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region:us-east-1}") String awsRegion) {
        return SqsClient.builder().region(Region.of(awsRegion)).build();
    }

    @Bean
    public LedgerControllerGrpc ledgerControllerGrpc(LedgerSimulacaoService simulacaoService,
                                                      LedgerEfetivacaoService efetivacaoService) {
        return new LedgerControllerGrpc(simulacaoService, efetivacaoService);
    }

    @Bean
    public RetornoContaControllerGrpc retornoContaControllerGrpc(RetornoContaRedisPublisher redisPublisher) {
        return new RetornoContaControllerGrpc(redisPublisher);
    }

    @Bean
    public RedisMessageListenerContainer redisListenerContainer(RedisConnectionFactory connectionFactory,
                                                                RetornoContaRedisSubscriber subscriber,
                                                                @Qualifier("instanceId") String instanceId) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new PatternTopic("channel:" + instanceId + ":*"));
        return container;
    }

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
