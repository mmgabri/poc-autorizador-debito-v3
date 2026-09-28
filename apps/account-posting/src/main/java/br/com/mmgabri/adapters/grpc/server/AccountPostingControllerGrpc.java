package br.com.mmgabri.adapters.grpc.server;

import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultResponse;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingResponse;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.mmgabri.errors.GrpcErrors;
import br.com.mmgabri.services.LedgerCommitService;
import br.com.mmgabri.services.LedgerSimulationService;
import br.com.mmgabri.services.MetricsService;
import br.com.mmgabri.services.PostingResultService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.time.Duration;
import java.time.OffsetDateTime;

@GrpcService
@RequiredArgsConstructor
public class AccountPostingControllerGrpc extends AccountPostingServiceGrpc.AccountPostingServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AccountPostingControllerGrpc.class);

    private final LedgerSimulationService ledgerSimulationService;
    private final LedgerCommitService ledgerCommitService;
    private final PostingResultService postingResultService;
    private final MetricsService metricsService;

    @Override
    public void requestPosting(RequestPostingRequest request, StreamObserver<RequestPostingResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.debug("Received requestPosting. operationType={}", request.getOperationType());
        try {
            var response = "COMMIT".equals(request.getOperationType())
                    ? ledgerCommitService.execute(request)
                    : ledgerSimulationService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime, request.getOperationType());

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on requestPosting: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (Exception e) {
            logger.error("Unexpected error on requestPosting", e);
            var error = GrpcErrors.toStatusException(ReasonCode.REASON_CODE_ACCOUNT_POSTING_INTERNAL_ERROR, Status.Code.INTERNAL);
            responseObserver.onError(error);
        }
    }

    @Override
    public void handlePostingResult(HandlePostingResultRequest request, StreamObserver<HandlePostingResultResponse> responseObserver) {
        postingResultService.execute(request);

        var response = HandlePostingResultResponse.newBuilder().setMessage("OK").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void onSuccess(OffsetDateTime startTime, String operationType) {
        var delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        metricsService.incrementMetric("app_ledger_duration_transaction", startTime, "method:requestPosting", "tipo_operacao:" + operationType);
        logger.info("Processing completed in {} ms", delay);
    }
}
