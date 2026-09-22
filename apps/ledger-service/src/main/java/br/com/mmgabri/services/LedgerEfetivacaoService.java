package br.com.mmgabri.services;

import br.com.mmgabri.adapters.dynamodb.mapper.ComandoContaMapper;
import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.adapters.sqs.ComandoContaSqsPublisher;
import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;
import br.com.mmgabri.grpc.ledger.v1.LedgerRequest;
import br.com.mmgabri.grpc.ledger.v1.LedgerResponse;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Efetivação via SQS + callback gRPC do conta, destravada pelo Redis pub/sub
 * (ver {@link br.com.mmgabri.adapters.grpc.server.RetornoContaControllerGrpc}
 * e {@link br.com.mmgabri.adapters.redis.LedgerCompletionRedisSubscriber}).
 */
@Service
@RequiredArgsConstructor
public class LedgerEfetivacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEfetivacaoService.class);

    private final ComandoContaRepository comandoContaRepository;
    private final ComandoContaMapper comandoContaMapper;
    private final ComandoContaSqsPublisher comandoContaSqsPublisher;
    private final PendingEfetivacaoRegistry pendingRegistry;
    private final MetricsService metricsService;
    private final String instanceId;

    // Aceita sufixo (30s, 500ms, 1m) via suporte nativo do Spring Boot pra Duration.
    @Value("${app.async.response-timeout}")
    private Duration responseTimeout;

    public LedgerResponse execute(LedgerRequest request) {
        String correlationId = request.getHeaderMessageGrpc().getCorrelationId();

        var pendingEntity = comandoContaMapper.toPendingEntity(request, instanceId);
        comandoContaRepository.insertPending(pendingEntity);

        CompletableFuture<RetornoContaRequest> future = pendingRegistry.register(correlationId);
        var sqsMessage = comandoContaMapper.toComandoContaRequest(request, instanceId);
        comandoContaSqsPublisher.publish(sqsMessage);
        logger.debug("Efetivação despachada via SQS para o conta. correlationId={}", correlationId);

        try {
            RetornoContaRequest retorno = future.get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS);
            logger.debug("Continuando processamento após retorno do conta. correlationId={}", correlationId);
            comandoContaRepository.updateCompletedAck(correlationId);
            return toLedgerResponse(request, retorno);
        } catch (TimeoutException e) {
            pendingRegistry.remove(correlationId);
            metricsService.incrementMetricCounter("app_timeout_efetivacao_conta");
            logger.warn("Timeout aguardando confirmação do conta. correlationId={}", correlationId);
            throw new StatusRuntimeException(Status.DEADLINE_EXCEEDED
                    .withDescription("Timeout aguardando confirmação do conta"));
        } catch (InterruptedException | ExecutionException e) {
            pendingRegistry.remove(correlationId);
            Thread.currentThread().interrupt();
            throw new StatusRuntimeException(Status.INTERNAL.withDescription("Erro aguardando confirmação do conta").withCause(e));
        }
    }

    private LedgerResponse toLedgerResponse(LedgerRequest request, RetornoContaRequest retorno) {
        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                .setCorrelationId(request.getHeaderMessageGrpc().getCorrelationId())
                .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                .setMessage(request.getHeaderMessageGrpc().getMessage())
                .build();

        return LedgerResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(request.getContaId())
                .setApproved(retorno.getApproved())
                .setErrorCode(retorno.getErrorCode())
                .setErrorDescription(retorno.getErrorDescription())
                .build();
    }
}
