package br.com.mmgabri.config;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public BindableService protoReflectionService() {
        return ProtoReflectionService.newInstance();
    }

    @Bean
    public GrpcServerExecutorProvider grpcServerExecutorProvider() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }

    // Único serviço com classes injetando AsyncTaskExecutor por tipo
    // (UseCaseAuthorization, TransactionContextRegistryService). O Boot 4.1
    // também auto-configura um "taskScheduler" compatível com esse tipo,
    // então precisamos de um bean explícito com @Primary para desambiguar
    // (tentamos @Qualifier no campo com @RequiredArgsConstructor, mas o
    // Lombok não copia essa anotação pro construtor gerado por padrão).
    @Bean
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    // Conexão Redis dedicada (não compartilhada) exclusiva pra comandos bloqueantes
    // (BLPOP com timeout fracionário via API nativa do Lettuce - ver
    // LedgerCompletionRedisClient). A conexão padrão (StringRedisTemplate) é
    // compartilhada entre toda a aplicação; usar BLPOP nela travaria qualquer
    // outro comando Redis concorrente até o BLPOP terminar. Nome do bean tem que
    // bater com o nome do parâmetro no @RequiredArgsConstructor de quem injeta,
    // já que @Qualifier não é copiado pelo Lombok pro construtor gerado.
    @Bean
    public LettuceConnectionFactory blockingRedisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port) {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(host, port);
        factory.setShareNativeConnection(false);
        return factory;
    }

}
