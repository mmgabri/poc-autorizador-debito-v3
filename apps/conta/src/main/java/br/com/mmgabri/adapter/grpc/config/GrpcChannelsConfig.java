package br.com.mmgabri.adapter.grpc.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcChannelsConfig {

    @Value("${grpc.ledger-service-client.host}")
    private String asyncBridgeHost;

    @Value("${grpc.ledger-service-client.port}")
    private int asyncBridgePort;

    @Bean
    public ManagedChannel managedChannelAsyncBridge() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + asyncBridgeHost + ":" + asyncBridgePort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }
}