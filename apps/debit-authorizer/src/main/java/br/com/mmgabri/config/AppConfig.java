package br.com.mmgabri.config;

import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerExecutorProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Bean
    public BindableService protoReflectionService() {
        return ProtoReflectionService.newInstance();
    }

    @Bean
    public GrpcServerExecutorProvider grpcServerExecutorProvider() {
        return Executors::newVirtualThreadPerTaskExecutor;
    }

    // Only service with classes injecting AsyncTaskExecutor by type
    // (UseCaseAuthorization, TransactionContextRegistryService). Boot 4.1 also
    // auto-configures a "taskScheduler" compatible with that type, so we need an
    // explicit @Primary bean to disambiguate (we tried @Qualifier on the field with
    // @RequiredArgsConstructor, but Lombok does not copy that annotation to the
    // generated constructor by default).
    @Bean
    @Primary
    public AsyncTaskExecutor applicationTaskExecutor() {
        var executor = new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
        executor.setTaskDecorator(new MdcTaskDecorator());
        return executor;
    }
}
