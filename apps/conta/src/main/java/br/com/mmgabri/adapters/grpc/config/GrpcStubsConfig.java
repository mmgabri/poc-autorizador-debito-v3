package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub retornoContaServiceStub(
            @Qualifier("managedChannelLedger") ManagedChannel channel) {
        return RetornoContaServiceGrpc.newBlockingV2Stub(channel);
    }
}
