package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.AntiFraudRequest;
import br.com.mmgabri.grpc.AntiFraudResponse;
import br.com.mmgabri.grpc.AntiFraudServiceGrpc;
import br.com.mmgabri.services.AntiFraudService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AntiFraudControllerGrpc extends AntiFraudServiceGrpc.AntiFraudServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AntiFraudControllerGrpc.class);

    private final AntiFraudService antiFraudService;

    @Override
    public void validarFraude(AntiFraudRequest request, StreamObserver<AntiFraudResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received validarFraude");
        try {
            var response = antiFraudService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on validarFraude: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on validarFraude", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Erro interno")
                            .asRuntimeException()
            );
        }
    }

    private void onSuccess(long startTime) {
        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
        logger.debug("Processamento concluído em {} (ms)", durationInMillis);
    }
}
