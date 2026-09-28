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

    // Boot 4.1 auto-configures a Jackson 3 ObjectMapper (tools.jackson.databind);
    // ReversalSqsAdapter uses classic Jackson 2 (com.fasterxml.jackson.databind)
    // directly, so it needs the explicit bean.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

}
