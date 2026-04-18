package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.*;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub segurancaServiceV2Stub(
            @Qualifier("managedChannelSeguranca") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return SegurancaServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub dataEnrichmentServiceBlockingV2Stub(
            @Qualifier("managedChannelDataEnrichment") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return DataEnrichmentServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LimiteServiceGrpc.LimiteServiceBlockingV2Stub limiteServiceStub(
            @Qualifier("managedChannelLimite") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LimiteServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LimitePortadorServiceGrpc.LimitePortadorServiceBlockingV2Stub limitePortadorServiceStub(
            @Qualifier("managedChannelLimitePortador") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LimitePortadorServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }

    @Bean
    public LancamentoContaServiceGrpc.LancamentoContaServiceBlockingV2Stub LancamentoContaServiceStub(
            @Qualifier("managedChannelLancamentoConta") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return LancamentoContaServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }
}