package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.RulesRequest;
import br.com.mmgabri.grpc.RulesResponse;
import br.com.mmgabri.grpc.RulesServiceGrpc;
import br.com.mmgabri.services.RulesService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class RulesControllerGrpc extends RulesServiceGrpc.RulesServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RulesControllerGrpc.class);

    private final RulesService rulesService;

    @Override
    public void validateRules(RulesRequest request, StreamObserver<RulesResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received validateRules");
        try {
            var response = rulesService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on validateRules: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on validateRules", e);
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
