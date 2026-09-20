package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.autorizador.v1.AutorizadorServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub autorizadorServiceV2Stub(
            @Qualifier("managedChannelAutorizador") ManagedChannel channel, ClientInterceptor grpcMetricsClientInterceptor) {
        return AutorizadorServiceGrpc.newBlockingV2Stub(channel)
                .withInterceptors(grpcMetricsClientInterceptor);
    }
}
