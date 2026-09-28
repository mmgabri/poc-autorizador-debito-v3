package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.*;
import br.com.mmgabri.adapters.grpc.mappers.AuthorizationMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enums.ServicesEnum;
import br.com.mmgabri.application.exceptions.ServiceAwareException;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.application.services.CompensationTransactionService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static br.com.mmgabri.application.domains.enums.AuthorizationStatusEnum.APPROVED;
import static br.com.mmgabri.application.domains.enums.AuthorizationStatusEnum.DENIED;
import static br.com.mmgabri.application.domains.enums.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class UseCaseAuthorization {

    private static final Logger logger = LoggerFactory.getLogger(UseCaseAuthorization.class);

    private final AuthorizationMapper authorizationMapper;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final RulesServiceGrpcClient rulesGrpcClient;
    private final SecurityServiceGrpcClient securityGrpcClient;
    private final LimitServiceGrpcClient limitGrpcClient;
    private final LedgerServiceGrpcClient ledgerGrpcClient;
    private final AntiFraudServiceGrpcClient antiFraudGrpcClient;
    private final CompensationTransactionService compensationTransactionService;
    private final TransactionContextRegistryService transactionContextRegistry;

    public AuthorizeTransactionResponse execute(Payload payload) {

        logger.debug("Initiating use case financial execution for product '{}'", payload.getProductDomain().getProductName());

        var transactionContext = transactionContextRegistry.initializeTransactionExecutionContext(payload);

        logger.debug("Phase 1 - parallel simulation calls");

        // ── Phase 1: SIMULATION (parallel) ───────────────────────────────────
        var rulesFuture = handleCompletion(supplyAsync(() -> rulesGrpcClient.execute(payload)), transactionContext, RULES_SERVICE);
        var securityFuture = handleCompletion(supplyAsync(() -> securityGrpcClient.execute(payload)), transactionContext, SECURITY_SERVICE);
        var limitSimulationFuture = handleCompletion(supplyAsync(() -> limitGrpcClient.execute(payload, "SIMULATION")), transactionContext, LIMIT_SERVICE_SIMULATION);
        var ledgerSimulationFuture = handleCompletion(supplyAsync(() -> ledgerGrpcClient.execute(payload, "SIMULATION")), transactionContext, LEDGER_SERVICE_SIMULATION);

        waitAll(rulesFuture, securityFuture, limitSimulationFuture, ledgerSimulationFuture);

        Optional<Throwable> phase1Error = findFirstError(rulesFuture, securityFuture, limitSimulationFuture, ledgerSimulationFuture);
        if (phase1Error.isPresent()) {
            Throwable error = phase1Error.get();
            logger.error("Phase 1 denied. Reason: {}", error.getMessage());
            return onCompleteTransactionDenied(transactionContext, payload, error);
        }

        logger.debug("Phase 1 approved - proceeding to Phase 2 COMMIT");

        // ── Phase 2: COMMIT (parallel) ───────────────────────────────────────
        var antiFraudFuture = handleCompletion(supplyAsync(() -> antiFraudGrpcClient.execute(payload)), transactionContext, ANTIFRAUD_SERVICE);
        var limitCommitFuture = handleCompletion(supplyAsync(() -> limitGrpcClient.execute(payload, "COMMIT")), transactionContext, LIMIT_SERVICE);
        var ledgerCommitFuture = handleCompletion(supplyAsync(() -> ledgerGrpcClient.execute(payload, "COMMIT")), transactionContext, LEDGER_SERVICE);
        waitAll(antiFraudFuture, limitCommitFuture, ledgerCommitFuture);

        if (allSucceeded(antiFraudFuture, limitCommitFuture, ledgerCommitFuture)) {
            return onCompleteTransactionApproved(transactionContext, payload);
        }

        // ── SAGA: at least one succeeded — publish compensation notification ──
        triggerSagaCompensation(antiFraudFuture, limitCommitFuture, ledgerCommitFuture, payload);

        Optional<Throwable> phase2Error = findFirstError(antiFraudFuture, limitCommitFuture, ledgerCommitFuture);
        var phase2Failure = phase2Error.orElseGet(() -> new TechnicalException("commit", ReasonCode.REASON_CODE_AUTHORIZER_INTERNAL_ERROR, "Phase 2 failed"));
        return onCompleteTransactionDenied(transactionContext, payload, phase2Failure);
    }

    private AuthorizeTransactionResponse onCompleteTransactionApproved(TransactionExecutionContext txCtx, Payload payload) {
        logger.debug("Use case financial completed - Transaction approved.");
        transactionContextRegistry.updateStatusTransactionContext(txCtx, APPROVED);
        return authorizationMapper.toSuccessResponse(payload);
    }

    private AuthorizeTransactionResponse onCompleteTransactionDenied(TransactionExecutionContext txCtx, Payload payload, Throwable throwable) {
        logger.debug("Use case financial completed - Transaction denied.");
        transactionContextRegistry.updateStatusTransactionContext(txCtx, DENIED);
        return authorizationMapper.toErrorResponse(payload, throwable);
    }

    private void triggerSagaCompensation(CompletableFuture<?> antiFraud, CompletableFuture<?> limit, CompletableFuture<?> ledger, Payload payload) {
        if (serviceSucceeded(antiFraud) || serviceSucceeded(limit) || serviceSucceeded(ledger)) {
            logger.warn("Phase 2 partial failure - publishing reversal notification");
            compensationTransactionService.publish(payload.getHeaderMessage().getTransactionId());
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private <T> CompletableFuture<T> supplyAsync(java.util.function.Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, asyncTaskExecutor);
    }

    /**
     * Chains a whenComplete callback on the future so that as soon as the service
     * responds (success or failure), the result is immediately logged and registered
     * in DynamoDB — without waiting for the other parallel calls to finish.
     */
    private <T> CompletableFuture<T> handleCompletion(CompletableFuture<T> future, TransactionExecutionContext txCtx, ServicesEnum service) {
        return future.whenComplete((ignored, ex) -> {
            if (ex == null) {
                logger.debug("Service '{}' completed successfully", service.getServiceName());
                transactionContextRegistry.registerServiceExecution(txCtx, service, APPROVED);
            } else {
                Throwable cause = unwrap(ex);
                logger.error("Service '{}' failed [{}]: {}", service.getServiceName(), cause.getClass().getSimpleName(), cause.getMessage());
                if (cause instanceof ServiceAwareException sae) {
                    transactionContextRegistry.registerServiceExecution(txCtx, sae);
                } else {
                    var unexpected = new TechnicalException(service.getServiceName(), ReasonCode.REASON_CODE_AUTHORIZER_INTERNAL_ERROR, cause.getMessage());
                    transactionContextRegistry.registerServiceExecution(txCtx, unexpected);
                }
            }
        });
    }

    private void waitAll(CompletableFuture<?>... futures) {
        CompletableFuture.allOf(
                Stream.of(futures)
                        .map(future -> future.exceptionally(ex -> null))
                        .toArray(CompletableFuture[]::new)
        ).join();
    }

    private Optional<Throwable> findFirstError(CompletableFuture<?>... futures) {
        return Arrays.stream(futures)
                .filter(CompletableFuture::isCompletedExceptionally)
                .map(f -> f.handle((ignored, ex) -> unwrap(ex)).join())
                .filter(Objects::nonNull)
                .findFirst();
    }

    private boolean allSucceeded(CompletableFuture<?>... futures) {
        return Stream.of(futures).noneMatch(CompletableFuture::isCompletedExceptionally);
    }

    private boolean serviceSucceeded(CompletableFuture<?> future) {
        return !future.isCompletedExceptionally();
    }

    private Throwable unwrap(Throwable ex) {
        return (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
    }
}