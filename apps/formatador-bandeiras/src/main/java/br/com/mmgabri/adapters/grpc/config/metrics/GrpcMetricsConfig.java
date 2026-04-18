package br.com.mmgabri.adapters.grpc.config.metrics;

import io.grpc.ClientInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcMetricsConfig {

    @Bean
    public ClientInterceptor grpcMetricsClientInterceptor(MeterRegistry meterRegistry) {
        return new GrpcMetricsClientInterceptor(meterRegistry);
    }
}
