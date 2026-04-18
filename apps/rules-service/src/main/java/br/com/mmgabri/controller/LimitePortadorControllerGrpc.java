package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.LimitePortadorRequest;
import br.com.mmgabri.grpc.LimitePortadorResponse;
import br.com.mmgabri.grpc.LimitePortadorServiceGrpc;
import br.com.mmgabri.services.LimitePortadorService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class LimitePortadorControllerGrpc extends LimitePortadorServiceGrpc.LimitePortadorServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LimitePortadorControllerGrpc.class);

    private final LimitePortadorService limitePortadorService;

    @Override
    public void atualizarLimitePortador(LimitePortadorRequest request, StreamObserver<LimitePortadorResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.info("Received atualizarLimitePortador");
        try {
            var response = limitePortadorService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on atualizarLimitePortador: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on atualizarLimitePortador", e);
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
        logger.info("Processamento concluído em {} (ms)", durationInMillis);
    }
}
