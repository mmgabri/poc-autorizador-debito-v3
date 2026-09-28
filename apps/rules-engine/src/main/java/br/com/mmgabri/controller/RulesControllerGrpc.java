package br.com.mmgabri.controller;

import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesRequest;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesResponse;
import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.mmgabri.services.RulesService;
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
public class RulesControllerGrpc extends RulesEngineServiceGrpc.RulesEngineServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RulesControllerGrpc.class);

    private final RulesService rulesService;

    @Override
    public void checkRules(CheckRulesRequest request, StreamObserver<CheckRulesResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.debug("Received checkRules");
        try {
            var response = rulesService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime);

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on checkRules: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (RuntimeException e) {
            logger.error("Unexpected error on checkRules", e);
            var error = GrpcErrors.toStatusException(ReasonCode.REASON_CODE_RULES_INTERNAL_ERROR, Status.Code.INTERNAL);
            responseObserver.onError(error);
        }
    }

    private void onSuccess(long startTime) {
        long endTime = System.nanoTime();
        long durationInMillis = (endTime - startTime) / 1_000_000;
        logger.debug("Processing completed in {} ms", durationInMillis);
    }
}
