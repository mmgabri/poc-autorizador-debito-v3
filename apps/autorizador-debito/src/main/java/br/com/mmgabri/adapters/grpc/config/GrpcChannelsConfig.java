package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class GrpcChannelsConfig {

    @Value("${grpc.enrichment-service-client.host}")
    private String dataEnrichmentHost;

    @Value("${grpc.enrichment-service-client.port}")
    private int dataEnrichmentPort;

    @Value("${grpc.rules-service-client.host}")
    private String rulesHost;

    @Value("${grpc.rules-service-client.port}")
    private int rulesPort;

    @Value("${grpc.security-service-client.host}")
    private String segurancaHost;

    @Value("${grpc.security-service-client.port}")
    private int segurancaPort;

    @Value("${grpc.limit-service-client.host}")
    private String limiteHost;

    @Value("${grpc.limit-service-client.port}")
    private int limitePort;

    @Value("${grpc.ledger-service-client.host}")
    private String ledgerHost;

    @Value("${grpc.ledger-service-client.port}")
    private int ledgerPort;

    @Value("${grpc.antifraud-service-client.host}")
    private String antiFraudHost;

    @Value("${grpc.antifraud-service-client.port}")
    private int antiFraudPort;

    @Bean
    public ManagedChannel managedChannelDataEnrichment() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + dataEnrichmentHost + ":" + dataEnrichmentPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelRules() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + rulesHost + ":" + rulesPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelSeguranca() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + segurancaHost + ":" + segurancaPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelLimite() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + limiteHost + ":" + limitePort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelLedger() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + ledgerHost + ":" + ledgerPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelAntiFraud() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + antiFraudHost + ":" + antiFraudPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }
}
