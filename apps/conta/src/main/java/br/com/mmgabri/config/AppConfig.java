package br.com.mmgabri.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    // Boot 4.1 auto-configura um ObjectMapper do Jackson 3 (tools.jackson.databind);
    // o ComandoContaSqsAdapter usa Jackson 2 clássico (com.fasterxml.jackson.databind)
    // diretamente, então precisa do bean explícito.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
