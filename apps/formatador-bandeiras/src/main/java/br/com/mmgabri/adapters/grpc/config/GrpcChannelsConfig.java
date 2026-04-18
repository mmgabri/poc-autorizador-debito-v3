package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcChannelsConfig {

    @Value("${grpc.autorizador-client.host}")
    private String autorizadorHost;

    @Value("${grpc.autorizador-client.port}")
    private int autorizadorPort;

    @Bean
    public ManagedChannel managedChannelAutorizador() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + autorizadorHost + ":" + autorizadorPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)  // Mantém a conexão ativa
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }
}