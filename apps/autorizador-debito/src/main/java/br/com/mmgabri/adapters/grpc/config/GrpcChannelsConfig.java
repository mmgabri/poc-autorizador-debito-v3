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

    @Value("${grpc.security-service-client.host}")
    private String segurancaHost;

    @Value("${grpc.security-service-client.port}")
    private int segurancaPort;

    @Value("${grpc.rules-service-client.host}")
    private String limitePortadorHost;

    @Value("${grpc.rules-service-client.port}")
    private int limitePortadorPort;

    @Value("${grpc.limit-service-client.host}")
    private String limiteHost;

    @Value("${grpc.limit-service-client.port}")
    private int limitePort;

    @Value("${grpc.ledger-service-client.host}")
    private String lancamentoContaHost;

    @Value("${grpc.ledger-service-client.port}")
    private int lancamentoContaPort;

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
    public ManagedChannel managedChannelSeguranca() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + segurancaHost + ":" + segurancaPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)  // Mantém a conexão ativa
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
                .keepAliveTime(60, TimeUnit.SECONDS)  // Mantém a conexão ativa
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelLimitePortador() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + limitePortadorHost + ":" + limitePortadorPort)
                .defaultLoadBalancingPolicy("round_robin")
                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)  // Mantém a conexão ativa
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ManagedChannel managedChannelLancamentoConta() {
        return ManagedChannelBuilder
                .forTarget("dns:///" + lancamentoContaHost + ":" + lancamentoContaPort)
                .defaultLoadBalancingPolicy("round_robin")
                                .usePlaintext()
                .keepAliveTime(60, TimeUnit.SECONDS)  // Mantém a conexão ativa
                .keepAliveTimeout(60, TimeUnit.SECONDS)
                .idleTimeout(60, TimeUnit.SECONDS)
                .build();
    }
}