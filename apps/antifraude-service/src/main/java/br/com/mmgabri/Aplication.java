package br.com.mmgabri;

import br.com.mmgabri.controller.AntiFraudeControllerGrpc;
import br.com.mmgabri.grpc.AntiFraudeServiceGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executors;

@SpringBootApplication
public class Aplication {
    public static void main(String[] args) throws Exception {
        ApplicationContext context = SpringApplication.run(Aplication.class, args);

        AntiFraudeServiceGrpc.AntiFraudeServiceImplBase antiFraudeGrpc =
                context.getBean(AntiFraudeControllerGrpc.class);

        Server server = ServerBuilder
                .forPort(58107)
                .addService(antiFraudeGrpc)
                .addService(ProtoReflectionService.newInstance())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()
                .start();

        System.out.println("gRPC Server iniciado na porta 58107");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
