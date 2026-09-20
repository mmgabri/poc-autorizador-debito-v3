package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisClient;
import br.com.mmgabri.adapters.sqs.ComandoContaSqsPublisher;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.enuns.CompletionTriggerEnum;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.LedgerEfetivacaoResult;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Efetivação do ledger não é uma chamada gRPC síncrona como a simulação: é
 * despachada via SQS e conciliada no DynamoDB de forma assíncrona pelo
 * ledger-service. Este serviço cuida das duas pontas — despacho e confirmação
 * best-effort via sinal do Redis + leitura do registro no DynamoDB.
 */
@Service
@RequiredArgsConstructor
public class LedgerEfetivacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEfetivacaoService.class);
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "completed";

    private final ComandoContaRepository comandoContaRepository;
    private final ComandoContaSqsPublisher comandoContaSqsPublisher;
    private final LedgerCompletionRedisClient ledgerCompletionRedisClient;

    @Value("${grpc.ledger-service-client.completion-timeout}")
    private long completionTimeoutMillis;

    @Value("${aws.dynamodb.comando-conta-pending-ttl-seconds}")
    private long pendingTtlSeconds;

    public void dispatch(Payload payload) {
        String correlationId = payload.getHeaderMessage().getCorrelationId();
        String contaId = payload.getDataEnrichment().getConta().getContaId();

        ComandoContaEntity entity = new ComandoContaEntity();
        entity.setCorrelationId(correlationId);
        entity.setStatus(STATUS_PENDING);
        entity.setContaId(contaId);
        entity.setUpdatedAt(OffsetDateTime.now().toString());
        entity.setExpiresAt(OffsetDateTime.now().plusSeconds(pendingTtlSeconds).toEpochSecond());
        comandoContaRepository.save(entity);

        ComandoContaRequest message = new ComandoContaRequest(
                correlationId,
                payload.getHeaderMessage().getTransactionId(),
                payload.getHeaderMessage().getBandeira(),
                payload.getHeaderMessage().getPlataforma(),
                payload.getHeaderMessage().getTimestamp(),
                payload.getHeaderMessage().getMessage(),
                contaId,
                payload.getExecutionSimulationConfig().getCustomReturnLedger(),
                payload.getExecutionSimulationConfig().getSleepLedgerEfetivacao()
        );
        comandoContaSqsPublisher.publish(message);

        logger.debug("Efetivação despachada via SQS. correlationId={}", correlationId);
    }

    /**
     * Aguarda até {@code grpc.ledger-service-client.completion-timeout} por um sinal
     * do ledger via Redis. Se sinalizar a tempo, confirma o resultado lendo o próprio
     * registro no DynamoDB (fonte da verdade) em vez de confiar em qualquer conteúdo
     * carregado no sinal.
     * <p>
     * Se não sinalizar a tempo, tenta marcar o registro como TIMEOUT — uma transição
     * atômica (condicionada a ainda estar PENDING) que resolve a corrida com o ledger:
     * se o ledger já tinha concluído um instante antes, a marcação falha e usamos o
     * resultado real dele em vez de negar por timeout às cegas.
     */
    public Optional<LedgerEfetivacaoResult> awaitCompletion(Payload payload) {
        String correlationId = payload.getHeaderMessage().getCorrelationId();

        boolean signaled = ledgerCompletionRedisClient.awaitSignal(correlationId, Duration.ofMillis(completionTimeoutMillis));
        if (signaled) {
            return readCompletedResult(correlationId, CompletionTriggerEnum.REDIS_SIGNAL);
        }

        logger.debug("Efetivação do ledger não confirmada dentro do timeout de {}ms. correlationId={}", completionTimeoutMillis, correlationId);
        if (comandoContaRepository.markTimeoutIfPending(correlationId)) {
            return Optional.empty();
        }

        logger.debug("TIMEOUT perdeu a corrida - ledger concluiu um instante antes. Usando o resultado real. correlationId={}", correlationId);
        return readCompletedResult(correlationId, CompletionTriggerEnum.LOST_TIMEOUT_RACE);
    }

    private Optional<LedgerEfetivacaoResult> readCompletedResult(String correlationId, CompletionTriggerEnum trigger) {
        Optional<ComandoContaEntity> entity = comandoContaRepository.findByCorrelationId(correlationId);
        if (entity.isEmpty() || !STATUS_COMPLETED.equalsIgnoreCase(entity.get().getStatus())) {
            logger.warn("{}, mas registro no DynamoDB não confirma conclusão. correlationId={}", trigger.getDescription(), correlationId);
            return Optional.empty();
        }

        ComandoContaEntity completed = entity.get();
        LedgerEfetivacaoResult result = new LedgerEfetivacaoResult(
                completed.getCorrelationId(),
                completed.getContaId(),
                Boolean.TRUE.equals(completed.getApproved()),
                completed.getErrorCode(),
                completed.getErrorDescription()
        );
        logger.debug("Ledger confirmou conclusão da efetivação. correlationId={} approved={}", correlationId, result.approved());
        return Optional.of(result);
    }
}
