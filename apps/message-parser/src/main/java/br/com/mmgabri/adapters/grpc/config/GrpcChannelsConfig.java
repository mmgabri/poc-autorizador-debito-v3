package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelsConfig {

    // Address/keepalive/idle-timeout come from
    // spring.grpc.client.channel.debit-authorizer.* (application.yml),
    // resolved by the native Boot 4.1 auto-configuration.
    @Bean
    public ManagedChannel managedChannelDebitAuthorizer(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("debit-authorizer");
    }
}
