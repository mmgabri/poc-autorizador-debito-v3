package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelsConfig {

    // Address/keepalive/idle-timeout of each channel come from
    // spring.grpc.client.channel.<name>.* (application.yml).

    @Bean
    public ManagedChannel managedChannelDataEnrichment(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("enrichment");
    }

    @Bean
    public ManagedChannel managedChannelRules(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("rules");
    }

    @Bean
    public ManagedChannel managedChannelSecurity(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("security");
    }

    @Bean
    public ManagedChannel managedChannelLimit(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("limit");
    }

    @Bean
    public ManagedChannel managedChannelLedger(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("ledger");
    }

    @Bean
    public ManagedChannel managedChannelAntiFraud(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("antifraud");
    }
}
