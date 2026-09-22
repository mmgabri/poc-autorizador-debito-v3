package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelsConfig {

    // Endereço/keepalive/idle-timeout do canal vem de
    // spring.grpc.client.channel.ledger.* (application.yml).
    @Bean
    public ManagedChannel managedChannelLedger(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("ledger");
    }
}
