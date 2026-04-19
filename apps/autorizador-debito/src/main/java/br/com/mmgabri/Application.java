package br.com.mmgabri;

import br.com.mmgabri.adapters.grpc.client.metrics.GrpcMetricsServerInterceptor;
import br.com.mmgabri.adapters.grpc.server.AutorizadorGrpcServer;
import br.com.mmgabri.config.ProductCatalogProperties;
import br.com.mmgabri.grpc.AutorizadorServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableConfigurationProperties(ProductCatalogProperties.class)
public class Application {
    public static void main(String[] args) throws Exception {

        ApplicationContext context = SpringApplication.run(Application.class, args);

        ServerInterceptor grpcMetricsServerInterceptor =
                context.getBean(GrpcMetricsServerInterceptor.class);

        AutorizadorServiceGrpc.AutorizadorServiceImplBase autorizadorGrpc =
                context.getBean(AutorizadorGrpcServer.class);

        int grpcPort = Integer.parseInt(context.getEnvironment().getRequiredProperty("grpc.server.port"));

        Server server = ServerBuilder
                .forPort(grpcPort)
                .addService(autorizadorGrpc)
                .addService(ProtoReflectionService.newInstance())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .intercept(grpcMetricsServerInterceptor)
                .build()
                .start();

        System.out.println("gRPC Server iniciado na porta " + grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();

    }
}

