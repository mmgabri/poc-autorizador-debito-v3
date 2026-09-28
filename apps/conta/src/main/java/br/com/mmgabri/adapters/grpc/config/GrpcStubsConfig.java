package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcStubsConfig {

    @Bean
    public AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub accountPostingServiceStub(
            @Qualifier("managedChannelLedger") ManagedChannel channel) {
        return AccountPostingServiceGrpc.newBlockingV2Stub(channel);
    }
}
