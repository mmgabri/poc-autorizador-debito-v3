package br.com.mmgabri.config;

import br.com.mmgabri.adapters.grpc.server.AutorizadorGrpcServer;
import br.com.mmgabri.application.ProcessTransaction;
import br.com.mmgabri.application.services.MetricsService;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public AutorizadorGrpcServer autorizadorControllerGrpc(ProcessTransaction processTransaction, MetricsService metricsService) {
        return new AutorizadorGrpcServer(processTransaction, metricsService);
    }

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

}
