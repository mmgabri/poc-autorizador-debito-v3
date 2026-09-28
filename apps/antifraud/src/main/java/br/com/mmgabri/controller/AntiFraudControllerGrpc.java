package br.com.mmgabri.controller;

import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudRequest;
import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudResponse;
import br.com.itau.debit.authorizer.antifraud.v1.AntifraudServiceGrpc;
import br.com.mmgabri.services.AntiFraudService;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.mmgabri.errors.GrpcErrors;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class AntiFraudControllerGrpc extends AntifraudServiceGrpc.AntifraudServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AntiFraudControllerGrpc.class);

    private final AntiFraudService antiFraudService;

    @Override
    public void analyzeFraud(AnalyzeFraudRequest request, StreamObserver<AnalyzeFraudResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received analyzeFraud");
        try {
            var response = antiFraudService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on analyzeFraud: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on analyzeFraud", e);
            var error = GrpcErrors.toStatusException(ReasonCode.REASON_CODE_ANTIFRAUD_INTERNAL_ERROR, Status.Code.INTERNAL);
            responseObserver.onError(error);
        }
    }

    private void onSuccess(long startTime) {
        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
        logger.debug("Processing completed in {} ms", durationInMillis);
    }
}
