package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.adapters.dynamodb.mapper.ComandoContaMapper;
import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisClient;
import br.com.mmgabri.adapters.sqs.ComandoContaSqsPublisher;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.LedgerEfetivacaoResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ComandoContaStatusEnum.*;
import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LEDGER_SERVICE;

/**
 * Efetivação do ledger não é uma chamada gRPC síncrona como a simulação: é
 * despachada via SQS e conciliada no DynamoDB de forma assíncrona pelo
 * ledger-service. Este serviço cuida das duas pontas — despacho e confirmação
 * via sinal do Redis, com a decisão de "o que aconteceu" sempre resolvida por
 * uma transição atômica (compare-and-swap) no comando_conta, nunca por uma
 * leitura simples.
 */
@Service
@RequiredArgsConstructor
public class LedgerEfetivacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEfetivacaoService.class);

    private final ComandoContaRepository comandoContaRepository;
    private final ComandoContaSqsPublisher comandoContaSqsPublisher;
    private final ComandoContaMapper comandoContaMapper;
    private final LedgerCompletionRedisClient ledgerCompletionRedisClient;
    private final MetricsService metricsService;


    @Value("${grpc.ledger-service-client.completion-timeout}")
    private long completionTimeoutMillis;

    public void dispatch(Payload payload) {
        String correlationId = payload.getHeaderMessage().getCorrelationId();
        String contaId = payload.getDataEnrichment().getConta().getContaId();

        ComandoContaEntity entity = comandoContaMapper.toPendingEntity(payload, contaId);
        if (!comandoContaRepository.insertPending(entity)) {
            logger.warn("Despacho abortado - comando já registrado pra esse correlationId. correlationId={}", correlationId);
            return;
        }

        ComandoContaRequest message = comandoContaMapper.toComandoContaRequest(payload, contaId);
        comandoContaSqsPublisher.publish(message);

        logger.debug("Efetivação despachada via SQS. correlationId={}", correlationId);
    }

    public LedgerEfetivacaoResponse awaitCompletion(Payload payload) {
        var startTime = OffsetDateTime.now();
        String correlationId = payload.getHeaderMessage().getCorrelationId();
        boolean signaled = ledgerCompletionRedisClient.awaitSignal(correlationId, Duration.ofMillis(completionTimeoutMillis));

        var fields = comandoContaMapper.toTransitionFields(correlationId);

        return signaled
                ? handleSignaled(correlationId, fields, startTime)
                : handleTimeout(correlationId, fields, startTime);
    }

    private LedgerEfetivacaoResponse handleSignaled(String correlationId, ComandoContaEntity fields, OffsetDateTime startTime) {
        logger.debug("Sinal de conclusão recebido do ledger via Redis. correlationId={}", correlationId);
        var result = comandoContaRepository.tryUpdateTransition(fields, COMPLETED, COMPLETED_ACK);
        return handleResponse(result.get(), startTime);
    }

    private LedgerEfetivacaoResponse handleTimeout(String correlationId, ComandoContaEntity fields, OffsetDateTime startTime) {
        logger.debug("Timeout aguardando sinal de conclusão do ledger via Redis. correlationId={}", correlationId);
        metricsService.incrementMetricCounter("app_timeout_ledger");
        var result = comandoContaRepository.tryUpdateTransition(fields, PENDING, TIMEOUT);
        if (result.get().getStatus().equals(COMPLETED)) {
            var result2 = comandoContaRepository.tryUpdateTransition(fields, COMPLETED, COMPLETED_ACK);
            metricsService.incrementMetricCounter("app_timeout_race_conditional_ledger");
            logger.info("Resposta do ledger recuperada do Dynamo apos expiracao do BLPOP. correlationId={}", correlationId);
            return handleResponse(result2.get(), startTime);
        }
        return handleResponse(result.get(), startTime);
    }

    private LedgerEfetivacaoResponse handleResponse(ComandoContaEntity response, OffsetDateTime startTime) {
        if (response.getStatus().equals(TIMEOUT)) {
            throw new BusinessException(LEDGER_SERVICE.getServiceName(), "999", "Timeout aguardando confirmação do ledger");
        }

        if (!response.getStatus().equals(COMPLETED_ACK)) {
            logger.error("Status inesperado do ledger: {}", response.getStatus());
            throw new BusinessException(LEDGER_SERVICE.getServiceName(), "999", "Status inesperado do ledger: " + response.getStatus());
        }

        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", LEDGER_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(LEDGER_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", LEDGER_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE.getServiceName(), "status:success");
        return comandoContaMapper.toLedgerEfetivacaoResponse(response);
    }
}
