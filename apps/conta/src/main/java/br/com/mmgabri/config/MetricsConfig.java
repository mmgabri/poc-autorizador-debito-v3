package br.com.mmgabri.config;

import com.timgroup.statsd.NonBlockingStatsDClientBuilder;
import com.timgroup.statsd.StatsDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public StatsDClient statsDClient(@Value("${DD_AGENT_HOST:localhost}") String ddAgentHost) {
        return new NonBlockingStatsDClientBuilder()
                .hostname(ddAgentHost)
                .port(8125)
                .build();
    }
}