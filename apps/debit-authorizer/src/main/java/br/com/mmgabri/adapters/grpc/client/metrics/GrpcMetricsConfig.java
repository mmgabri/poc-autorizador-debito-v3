package br.com.mmgabri.adapters.grpc.client.metrics;

import io.grpc.ClientInterceptor;
import io.grpc.ServerInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcMetricsConfig {

    @Bean
    public ServerInterceptor grpcMetricsServerInterceptor(MeterRegistry meterRegistry) {
        return new GrpcMetricsServerInterceptor(meterRegistry);
    }

    @Bean
    public ClientInterceptor grpcMetricsClientInterceptor(MeterRegistry meterRegistry) {
        return new GrpcMetricsClientInterceptor(meterRegistry);
    }
}
