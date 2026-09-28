package br.com.mmgabri.controller;

import br.com.itau.debit.authorizer.limit.v1.UpdateLimitRequest;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitResponse;
import br.com.itau.debit.authorizer.limit.v1.LimitServiceGrpc;
import br.com.mmgabri.services.LimitService;
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
public class LimitControllerGrpc extends LimitServiceGrpc.LimitServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LimitControllerGrpc.class);

    private final LimitService limitService;

    @Override
    public void updateLimit(UpdateLimitRequest request, StreamObserver<UpdateLimitResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received updateLimit");
        try {
            var response = limitService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on updateLimit: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on updateLimit", e);
            var error = GrpcErrors.toStatusException(ReasonCode.REASON_CODE_LIMIT_INTERNAL_ERROR, Status.Code.INTERNAL);
            responseObserver.onError(error);
        }
    }

    private void onSuccess(long startTime) {
        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
        logger.debug("Processing completed in {} ms", durationInMillis);
    }
}
