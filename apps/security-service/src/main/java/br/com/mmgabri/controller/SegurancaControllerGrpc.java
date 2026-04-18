package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.SegurancaRequest;
import br.com.mmgabri.grpc.SegurancaResponse;
import br.com.mmgabri.grpc.SegurancaServiceGrpc;
import br.com.mmgabri.services.MetricsService;
import br.com.mmgabri.services.SegurancaService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class SegurancaControllerGrpc extends SegurancaServiceGrpc.SegurancaServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SegurancaControllerGrpc.class);

    private final SegurancaService segurancaService;
    private final MetricsService metricsService;

    @Override
    public void validarSeguranca(SegurancaRequest request, StreamObserver<SegurancaResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.info("Received validarSeguranca");
        try {
            var response = segurancaService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on validarSeguranca: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on validarSeguranca", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Erro interno")
                            .asRuntimeException()
            );
        }
    }

    private void onSuccess(OffsetDateTime startTime) {
        logger.info("Processamento concluído em {} (ms)", Duration.between(startTime, OffsetDateTime.now()).toMillis());
        metricsService.incrementMetric("app_seg_duration_transaction", startTime);
    }
}
