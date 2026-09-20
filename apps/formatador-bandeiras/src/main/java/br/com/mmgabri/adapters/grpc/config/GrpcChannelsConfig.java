package br.com.mmgabri.adapters.grpc.config;

import io.grpc.ManagedChannel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

@Configuration
public class GrpcChannelsConfig {

    // O endereço/keepalive/idle-timeout agora vêm de
    // spring.grpc.client.channel.autorizador.* (application.yml),
    // resolvidos pela auto-configuração nativa do Boot 4.1.
    @Bean
    public ManagedChannel managedChannelAutorizador(GrpcChannelFactory channelFactory) {
        return channelFactory.createChannel("autorizador");
    }
}
