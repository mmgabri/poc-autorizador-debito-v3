package br.com.mmgabri.config;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
