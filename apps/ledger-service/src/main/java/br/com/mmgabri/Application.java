package br.com.mmgabri;

import br.com.mmgabri.adapters.grpc.server.LedgerControllerGrpc;
import br.com.mmgabri.adapters.grpc.server.RetornoContaControllerGrpc;
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

        LedgerControllerGrpc ledgerGrpc =
                context.getBean(LedgerControllerGrpc.class);
        RetornoContaControllerGrpc retornoContaGrpc =
                context.getBean(RetornoContaControllerGrpc.class);

        int grpcPort = Integer.parseInt(context.getEnvironment().getRequiredProperty("grpc.server.port"));

        Server server = ServerBuilder
                .forPort(grpcPort)
                .addService(ledgerGrpc)
                .addService(retornoContaGrpc)
                .addService(ProtoReflectionService.newInstance())
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build()
                .start();

        System.out.println("gRPC Server iniciado na porta " + grpcPort);

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
        server.awaitTermination();
    }
}
