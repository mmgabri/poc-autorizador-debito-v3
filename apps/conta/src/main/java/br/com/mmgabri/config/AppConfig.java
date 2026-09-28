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

    // Boot 4.1 auto-configures a Jackson 3 ObjectMapper (tools.jackson.databind);
    // AccountCommandSqsAdapter uses classic Jackson 2 (com.fasterxml.jackson.databind)
    // directly, so it needs the explicit bean.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
