package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingResponse;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.mmgabri.adapters.dynamodb.mapper.AccountCommandMapper;
import br.com.mmgabri.adapters.dynamodb.repository.AccountCommandRepository;
import br.com.mmgabri.adapters.sqs.AccountCommandSqsPublisher;
import br.com.mmgabri.errors.GrpcErrors;
import io.grpc.Status;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Commit via SQS + conta gRPC callback, released by Redis pub/sub
 * (see {@link br.com.mmgabri.services.PostingResultService}
 * and {@link br.com.mmgabri.adapters.redis.LedgerCompletionRedisSubscriber}).
 */
@Service
@RequiredArgsConstructor
public class LedgerCommitService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCommitService.class);

    private final AccountCommandRepository accountCommandRepository;
    private final AccountCommandMapper accountCommandMapper;
    private final AccountCommandSqsPublisher accountCommandSqsPublisher;
    private final PendingCommitRegistry pendingRegistry;
    private final MetricsService metricsService;
    private final String instanceId;

    // Accepts a suffix (30s, 500ms, 1m) via Spring Boot's native Duration support.
    @Value("${app.async.response-timeout}")
    private Duration responseTimeout;

    public RequestPostingResponse execute(RequestPostingRequest request) {
        String correlationId = request.getCorrelationId();

        var pendingEntity = accountCommandMapper.toPendingEntity(request, instanceId);
        accountCommandRepository.insertPending(pendingEntity);

        CompletableFuture<HandlePostingResultRequest> future = pendingRegistry.register(correlationId);
        var command = accountCommandMapper.toAccountCommand(request, instanceId);
        try {
            accountCommandSqsPublisher.publish(command);
        } catch (RuntimeException e) {
            pendingRegistry.remove(correlationId);
            logger.error("Failed to publish command to SQS. correlationId={}", correlationId, e);
            throw GrpcErrors.toStatusException(ReasonCode.REASON_CODE_ACCOUNT_POSTING_COMMAND_PUBLISH_FAILED, Status.Code.UNAVAILABLE);
        }
        logger.debug("Commit dispatched via SQS to conta. correlationId={}", correlationId);

        var startTime = OffsetDateTime.now();
        HandlePostingResultRequest postingResult;
        try {
            postingResult = future.get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingRegistry.remove(correlationId);
            metricsService.incrementMetricCounter("app_ledger_timeout_callback");
            logger.warn("Timeout waiting for conta confirmation. correlationId={}", correlationId);
            // Uncertain outcome: conta may have committed after the timeout.
            throw GrpcErrors.toStatusException(ReasonCode.REASON_CODE_ACCOUNT_POSTING_CALLBACK_TIMEOUT, Status.Code.DEADLINE_EXCEEDED);
        } catch (InterruptedException | ExecutionException e) {
            pendingRegistry.remove(correlationId);
            Thread.currentThread().interrupt();
            logger.error("Error waiting for conta confirmation. correlationId={}", correlationId, e);
            throw GrpcErrors.toStatusException(ReasonCode.REASON_CODE_ACCOUNT_POSTING_INTERNAL_ERROR, Status.Code.INTERNAL, "Error waiting for conta confirmation");
        }

        metricsService.incrementMetric("app_ledger_duration_call_async", startTime, "tipo_operacao:COMMIT");
        logger.debug("Resuming processing after conta response. correlationId={}", correlationId);
        accountCommandRepository.updateCompletedAck(correlationId);

        if (postingResult.hasTechnicalError()) {
            var technicalError = postingResult.getTechnicalError();
            logger.warn("Conta reported a technical failure. correlationId={} reasonCode={}", correlationId, technicalError.getReasonCode());
            var statusCode = statusCodeOf(technicalError.getReasonCode());
            throw GrpcErrors.toStatusException(technicalError.getReasonCode(), statusCode, technicalError.getMessage());
        }
        return toRequestPostingResponse(request, postingResult.getBusinessResult());
    }

    /**
     * gRPC status for the technical failure reported by conta in the callback.
     */
    private Status.Code statusCodeOf(ReasonCode reasonCode) {
        return switch (reasonCode) {
            case REASON_CODE_ACCOUNT_DEPENDENCY_UNAVAILABLE,
                 REASON_CODE_ACCOUNT_DEPENDENCY_TIMEOUT -> Status.Code.UNAVAILABLE;
            default -> Status.Code.INTERNAL;
        };
    }

    private RequestPostingResponse toRequestPostingResponse(RequestPostingRequest request, BusinessResult result) {
        return RequestPostingResponse.newBuilder()
                .setAccountId(request.getAccountId())
                .setResult(result)
                .build();
    }
}
