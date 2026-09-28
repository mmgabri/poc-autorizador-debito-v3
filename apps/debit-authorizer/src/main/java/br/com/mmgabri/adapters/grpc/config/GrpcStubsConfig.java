package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.antifraud.v1.AntifraudServiceGrpc;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichmentServiceGrpc;
import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import br.com.itau.debit.authorizer.limit.v1.LimitServiceGrpc;
import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.itau.debit.authorizer.security.v1.SecurityServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public EnrichmentServiceGrpc.EnrichmentServiceBlockingV2Stub dataEnrichmentServiceBlockingV2Stub(
            @Qualifier("managedChannelDataEnrichment") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return EnrichmentServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub rulesServiceStub(
            @Qualifier("managedChannelRules") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return RulesEngineServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public SecurityServiceGrpc.SecurityServiceBlockingV2Stub securityServiceStub(
            @Qualifier("managedChannelSecurity") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return SecurityServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LimitServiceGrpc.LimitServiceBlockingV2Stub limitServiceStub(
            @Qualifier("managedChannelLimit") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LimitServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub ledgerServiceStub(
            @Qualifier("managedChannelLedger") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return AccountPostingServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public AntifraudServiceGrpc.AntifraudServiceBlockingV2Stub antiFraudServiceStub(
            @Qualifier("managedChannelAntiFraud") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return AntifraudServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }
}
