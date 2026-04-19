package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.LimiteRequest;
import br.com.mmgabri.grpc.LimiteResponse;
import br.com.mmgabri.grpc.LimiteServiceGrpc;
import br.com.mmgabri.services.LimitService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class LimitControllerGrpc extends LimiteServiceGrpc.LimiteServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LimitControllerGrpc.class);

    private final LimitService limiteService;

    @Override
    public void atualizarLimite(LimiteRequest request, StreamObserver<LimiteResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received atualizarLimite");
        try {
            var response = limiteService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on atualizarLimite: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on atualizarLimite", e);
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
