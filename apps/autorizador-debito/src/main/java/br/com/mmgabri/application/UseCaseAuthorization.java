package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.*;
import br.com.mmgabri.adapters.grpc.mappers.AutorizadorMapper;
import br.com.mmgabri.application.services.CompensationTransactionService;
import br.com.mmgabri.application.services.LedgerEfetivacaoService;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.exceptions.ServiceAwareException;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.domain.LedgerEfetivacaoResult;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorResponse;
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

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.APPROVED;
import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.DENIED;
import static br.com.mmgabri.application.domains.enuns.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class UseCaseAuthorization {

    private static final Logger logger = LoggerFactory.getLogger(UseCaseAuthorization.class);

    private final AutorizadorMapper autorizadorMapper;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final RulesServiceGrpcClient rulesGrpcClient;
    private final SecurityServiceGrpcClient segurancaGrpcClient;
    private final LimitServiceGrpcClient limiteGrpcClient;
    private final LedgerServiceGrpcClient ledgerGrpcClient;
    private final AntiFraudServiceGrpcClient antiFraudGrpcClient;
    private final CompensationTransactionService compensationTransactionService;
    private final TransactionContextRegistryService transactionContextRegistry;
    private final LedgerEfetivacaoService ledgerEfetivacaoService;

    public AutorizadorResponse execute(Payload payload) {

        logger.debug("Initiating use case financial execution for product '{}'", payload.getProductDomain().getProductName());

        var transactionContext = transactionContextRegistry.initializeTransactionExecutionContext(payload);

        logger.debug("Phase 1 - parallel simulation calls");

        // ── Phase 1: SIMULACAO (parallel) ────────────────────────────────────
        var rulesFuture = handleCompletion(supplyAsync(() -> rulesGrpcClient.execute(payload)), transactionContext, RULES_SERVICE);
        var securityFuture = handleCompletion(supplyAsync(() -> segurancaGrpcClient.execute(payload)), transactionContext, SECURITY_SERVICE);
        var limitSimuFuture = handleCompletion(supplyAsync(() -> limiteGrpcClient.execute(payload, "SIMULACAO")), transactionContext, LIMIT_SERVICE_SIMULATION);
        var ledgerSimuFuture = handleCompletion(supplyAsync(() -> ledgerGrpcClient.execute(payload)), transactionContext, LEDGER_SERVICE_SIMULATION);

        waitAll(rulesFuture, securityFuture, limitSimuFuture, ledgerSimuFuture);

        Optional<Throwable> phase1Error = findFirstError(rulesFuture, securityFuture, limitSimuFuture, ledgerSimuFuture);
        if (phase1Error.isPresent()) {
            Throwable error = phase1Error.get();
            logger.error("Phase 1 denied. Reason: {}", error.getMessage());
            return onCompleteTransactionDenied(transactionContext, payload, error);
        }

        logger.debug("Phase 1 approved - proceeding to Phase 2 EFETIVACAO");

        // ── Phase 2: EFETIVACAO (parallel) ───────────────────────────────────
        dispatchLedgerEfetivacaoAsync(payload);
        var antiFraudFuture = handleCompletion(supplyAsync(() -> antiFraudGrpcClient.execute(payload)), transactionContext, ANTIFRAUD_SERVICE);
        var limitEfetFuture = handleCompletion(supplyAsync(() -> limiteGrpcClient.execute(payload, "EFETIVACAO")), transactionContext, LIMIT_SERVICE);

        waitAll(antiFraudFuture, limitEfetFuture);

        var ledgerResult = ledgerEfetivacaoService.awaitCompletion(payload);
        registerLedgerCompletion(transactionContext, ledgerResult);

        if (allSucceeded(ledgerResult, antiFraudFuture, limitEfetFuture)) {
            return onCompleteTransactionApproved(transactionContext, payload);
        }

        // ── SAGA: at least one succeeded — publish compensation notification ──
        triggerSagaCompensation(ledgerResult, antiFraudFuture, limitEfetFuture, payload);

        Optional<Throwable> phase2Error = findFirstError(ledgerResult, antiFraudFuture, limitEfetFuture);
        return onCompleteTransactionDenied(transactionContext, payload, phase2Error.orElseGet(() -> new BusinessException("efetivacao", "999", "Phase 2 failed")));
    }

    private AutorizadorResponse onCompleteTransactionApproved(TransactionExecutionContext txCtx, Payload payload) {
        logger.debug("Use case financial completed - Transaction approved.");
        transactionContextRegistry.updateStatusTransactionContext(txCtx, APPROVED);
        return autorizadorMapper.toAutorizadorResponseSuccess(payload);
    }

    private AutorizadorResponse onCompleteTransactionDenied(TransactionExecutionContext txCtx, Payload payload, Throwable throwable) {
        logger.debug("Use case financial completed - Transaction denied.");
        transactionContextRegistry.updateStatusTransactionContext(txCtx, DENIED);
        return autorizadorMapper.toAutorizadorResponseError(payload, throwable);
    }

    private void dispatchLedgerEfetivacaoAsync(Payload payload) {
        asyncTaskExecutor.execute(() -> {
            try {
                ledgerEfetivacaoService.dispatch(payload);
            } catch (Exception e) {
                logger.error("Falha ao despachar efetivação do ledger. correlationId={}", payload.getHeaderMessage().getCorrelationId(), e);
            }
        });
    }

    // Resultado do ledger não é mais um future — é um Optional já resolvido (BLPOP +
    // confirmação no DynamoDB). Este helper centraliza a regra de sucesso/falha dele
    // (vazio = timeout, approved=false = negado) num único lugar.
    private Optional<BusinessException> ledgerFailure(Optional<LedgerEfetivacaoResult> ledgerResult) {
        if (ledgerResult.isPresent() && ledgerResult.get().approved()) {
            return Optional.empty();
        }
        return Optional.of(ledgerResult
                .map(r -> new BusinessException(LEDGER_SERVICE.getServiceName(), r.errorCode(), r.errorDescription()))
                .orElseGet(() -> new BusinessException(LEDGER_SERVICE.getServiceName(), "999", "Efetivação do ledger não confirmada dentro do timeout")));
    }

    private void registerLedgerCompletion(TransactionExecutionContext txCtx, Optional<LedgerEfetivacaoResult> ledgerResult) {
        Optional<BusinessException> failure = ledgerFailure(ledgerResult);
        if (failure.isEmpty()) {
            logger.debug("Service '{}' completed successfully", LEDGER_SERVICE.getServiceName());
            transactionContextRegistry.registerServiceExecution(txCtx, LEDGER_SERVICE, APPROVED);
        } else {
            logger.error("Service '{}' failed: {}", LEDGER_SERVICE.getServiceName(), failure.get().getMessage());
            transactionContextRegistry.registerServiceExecution(txCtx, failure.get());
        }
    }

    private boolean allSucceeded(Optional<LedgerEfetivacaoResult> ledgerResult, CompletableFuture<?>... futures) {
        return ledgerFailure(ledgerResult).isEmpty() && allSucceeded(futures);
    }

    private boolean allSucceeded(CompletableFuture<?>... futures) {
        return Stream.of(futures).noneMatch(CompletableFuture::isCompletedExceptionally);
    }

    private Optional<Throwable> findFirstError(Optional<LedgerEfetivacaoResult> ledgerResult, CompletableFuture<?>... futures) {
        return ledgerFailure(ledgerResult)
                .<Throwable>map(e -> e)
                .or(() -> findFirstError(futures));
    }

    private void triggerSagaCompensation(Optional<LedgerEfetivacaoResult> ledgerResult, CompletableFuture<?> antiFraud, CompletableFuture<?> limit, Payload payload) {
        boolean ledgerSucceeded = ledgerFailure(ledgerResult).isEmpty();
        if (serviceSucceeded(antiFraud) || serviceSucceeded(limit) || ledgerSucceeded) {
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
                    transactionContextRegistry.registerServiceExecution(txCtx, new BusinessException(service.getServiceName(), "999", cause.getMessage()));
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

    private boolean serviceSucceeded(CompletableFuture<?> future) {
        return !future.isCompletedExceptionally();
    }

    private Throwable unwrap(Throwable ex) {
        return (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null) ? ex.getCause() : ex;
    }
}