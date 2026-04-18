package br.com.mmgabri;

import br.com.mmgabri.controller.DataEnrichmentControllerGrpc;
import br.com.mmgabri.grpc.DataEnrichmentServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executors;

@SpringBootApplication
public class Application {
    public static void main(String[] args) throws Exception {

        ApplicationContext context = SpringApplication.run(Application.class, args);

        DataEnrichmentServiceGrpc.DataEnrichmentServiceImplBase dataEnrichment =
                context.getBean(DataEnrichmentControllerGrpc.class);

        Server server = ServerBuilder
                .forPort(58082)
                .addService(dataEnrichment)
                .addService(ProtoReflectionService.newInstance())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()
                .start();

        System.out.println("gRPC Server iniciado na porta 58082");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();

    }
}

