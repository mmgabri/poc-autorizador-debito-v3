package br.com.mmgabri.adapters.grpc.server;

import br.com.mmgabri.application.ProcessTransaction;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.AutorizadorResponse;
import br.com.mmgabri.grpc.AutorizadorServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class AutorizadorGrpcServer extends AutorizadorServiceGrpc.AutorizadorServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AutorizadorGrpcServer.class);

    private final ProcessTransaction processTransaction;
    private final MetricsService metricsService;


    @Override
    public void autorizarTransacao(AutorizadorRequest request, StreamObserver<AutorizadorResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.info("Incoming gRPC request: autorizarTransacao. transactionId={}", request.getHeaderMessageGrpc().getCorrelationId());

        try {
            var response = processTransaction.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime, response);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error in autorizarTransacao: status={}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error in authorizeTransaction", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Erro interno")
                            .asRuntimeException()
            );
        }
    }

    private void onSuccess(OffsetDateTime startTime, AutorizadorResponse response) {
        logger.info("Authorization completed in {} ms", Duration.between(startTime, OffsetDateTime.now()).toMillis());
        metricsService.incrementMetric("app_duration_transaction", startTime, "status:"+response.getMessageIsoMap().get("039"));
    }
}
