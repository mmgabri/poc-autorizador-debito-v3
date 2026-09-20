package br.com.mmgabri.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public SqsClient sqsClient(@Value("${aws.sqs.region:us-east-1}") String awsRegion) {
        return SqsClient.builder().region(Region.of(awsRegion)).build();
    }

    @Bean
    public BindableService protoReflectionService() {
        return ProtoReflectionService.newInstance();
    }

    // Boot 4.1 auto-configura um ObjectMapper do Jackson 3 (tools.jackson.databind);
    // este serviço usa Jackson 2 clássico (com.fasterxml.jackson.databind) diretamente
    // no ComandoContaSqsAdapter, então precisa do bean explícito.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public GrpcServerExecutorProvider grpcServerExecutorProvider() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }

}
