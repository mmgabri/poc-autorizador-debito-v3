package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.DataEnrichmentServiceGrpc;
import br.com.mmgabri.grpc.EnrichByCardRequest;
import br.com.mmgabri.grpc.EnrichByCardResponse;
import br.com.mmgabri.services.DataEnrichmentService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class DataEnrichmentControllerGrpc extends DataEnrichmentServiceGrpc.DataEnrichmentServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(DataEnrichmentControllerGrpc.class);

    private final DataEnrichmentService dataEnrichmentService;

    @Override
    public void enrichByCard(EnrichByCardRequest request, StreamObserver<EnrichByCardResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.info("Received enrichByCard");
        try {
            var response = dataEnrichmentService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on enrichByCard: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on enrichByCard", e);
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
