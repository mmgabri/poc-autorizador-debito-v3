package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.antifraud.v1.AntiFraudServiceGrpc;
import br.com.mmgabri.grpc.enrichment.v1.DataEnrichmentServiceGrpc;
import br.com.mmgabri.grpc.ledger.v1.LedgerServiceGrpc;
import br.com.mmgabri.grpc.limit.v1.LimiteServiceGrpc;
import br.com.mmgabri.grpc.rules.v1.RulesServiceGrpc;
import br.com.mmgabri.grpc.security.v1.SegurancaServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub dataEnrichmentServiceBlockingV2Stub(
            @Qualifier("managedChannelDataEnrichment") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return DataEnrichmentServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public RulesServiceGrpc.RulesServiceBlockingV2Stub rulesServiceStub(
            @Qualifier("managedChannelRules") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return RulesServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub segurancaServiceV2Stub(
            @Qualifier("managedChannelSeguranca") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return SegurancaServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LimiteServiceGrpc.LimiteServiceBlockingV2Stub limiteServiceStub(
            @Qualifier("managedChannelLimite") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LimiteServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LedgerServiceGrpc.LedgerServiceBlockingV2Stub ledgerServiceStub(
            @Qualifier("managedChannelLedger") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LedgerServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public AntiFraudServiceGrpc.AntiFraudServiceBlockingV2Stub antiFraudServiceStub(
            @Qualifier("managedChannelAntiFraud") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return AntiFraudServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }
}
