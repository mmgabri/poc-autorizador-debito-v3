package br.com.mmgabri.controller;

import br.com.mmgabri.grpc.enrichment.v1.DataEnrichmentServiceGrpc;
import br.com.mmgabri.grpc.enrichment.v1.EnrichByCardRequest;
import br.com.mmgabri.grpc.enrichment.v1.EnrichByCardResponse;
import br.com.mmgabri.services.EnrichmentService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class EnrichmentControllerGrpc extends DataEnrichmentServiceGrpc.DataEnrichmentServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(EnrichmentControllerGrpc.class);

    private final EnrichmentService enrichmentService;

    @Override
    public void enrichByCard(EnrichByCardRequest request, StreamObserver<EnrichByCardResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received enrichByCard");
        try {
            var response = enrichmentService.execute(request);
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
        logger.debug("Processamento concluído em {} (ms)", durationInMillis);
    }
}
