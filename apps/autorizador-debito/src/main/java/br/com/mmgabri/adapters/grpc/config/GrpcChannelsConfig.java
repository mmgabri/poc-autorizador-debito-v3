package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelsConfig {

    // Endereço/keepalive/idle-timeout de cada canal agora vêm de
    // spring.grpc.client.channel.<nome>.* (application.yml).

    @Bean
    public ManagedChannel managedChannelDataEnrichment(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("enrichment");
    }

    @Bean
    public ManagedChannel managedChannelRules(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("rules");
    }

    @Bean
    public ManagedChannel managedChannelSeguranca(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("security");
    }

    @Bean
    public ManagedChannel managedChannelLimite(GrpcChannelFactory channelFactory) {
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
