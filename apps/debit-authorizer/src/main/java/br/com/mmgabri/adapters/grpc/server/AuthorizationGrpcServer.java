package br.com.mmgabri.adapters.grpc.server;

import br.com.mmgabri.application.ProcessTransaction;
import br.com.mmgabri.application.services.MetricsService;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizationServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.OffsetDateTime;

@GrpcService
@RequiredArgsConstructor
public class AuthorizationGrpcServer extends AuthorizationServiceGrpc.AuthorizationServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationGrpcServer.class);

    private final ProcessTransaction processTransaction;
    private final MetricsService metricsService;


    @Override
    public void authorizeTransaction(AuthorizeTransactionRequest request, StreamObserver<AuthorizeTransactionResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.debug("Incoming gRPC request: authorizeTransaction. correlationId={}", request.getCorrelationId());

        try {
            var response = processTransaction.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime, response);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error in authorizeTransaction: status={}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error in authorizeTransaction", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Internal error")
                            .asRuntimeException()
            );
        }
    }

    private void onSuccess(OffsetDateTime startTime, AuthorizeTransactionResponse response) {
        logger.info("Authorization completed in {} ms", Duration.between(startTime, OffsetDateTime.now()).toMillis());
        metricsService.incrementMetric("app_duration_transaction", startTime, "status:"+response.getMessageIsoMap().get("039"));
        metricsService.incrementMetricCounter("app_qtd_transaction", "status:"+response.getMessageIsoMap().get("039"));
    }
}
