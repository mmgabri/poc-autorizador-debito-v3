package br.com.mmgabri.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {
    @Bean(destroyMethod = "close")
    public ExecutorService vtExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // Boot 4.1 auto-configura um ObjectMapper do Jackson 3 (tools.jackson.databind);
    // o ReversalSqsAdapter usa Jackson 2 clássico (com.fasterxml.jackson.databind)
    // diretamente, então precisa do bean explícito.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
