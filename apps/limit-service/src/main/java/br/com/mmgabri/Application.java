package br.com.mmgabri;

import br.com.mmgabri.controller.LimiteControllerGrpc;
import br.com.mmgabri.grpc.LimiteServiceGrpc;
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

        LimiteServiceGrpc.LimiteServiceImplBase limiteGrpc =
                context.getBean(LimiteControllerGrpc.class);

        Server server = ServerBuilder
                .forPort(58085)
                .addService(limiteGrpc)
                .addService(ProtoReflectionService.newInstance())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()
                .start();

        System.out.println("gRPC Server iniciado na porta 58085");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();

    }
}

