package br.com.mmgabri.adapter.grpc.config;

import br.com.mmgabri.grpc.RetornoContaServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub asyncBridgeServiceBlockingV2Stub(
            @Qualifier("managedChannelAsyncBridge") ManagedChannel channel
    ) {
        return RetornoContaServiceGrpc.newBlockingV2Stub(channel);
    }
}
