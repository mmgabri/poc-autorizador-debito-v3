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
        ManagedChannel channel = ManagedChannelBuilder
                .forTarget("dns:///" + asyncBridgeHost + ":" + asyncBridgePort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .idleTimeout(600, TimeUnit.SECONDS)
                .build();
        channel.getState(true);
        return channel;
    }
}