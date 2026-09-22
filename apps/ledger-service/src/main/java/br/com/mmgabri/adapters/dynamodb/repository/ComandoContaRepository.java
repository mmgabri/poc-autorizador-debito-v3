package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.domain.enuns.ComandoContaStatusEnum;
import br.com.mmgabri.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.time.Duration;
import java.time.OffsetDateTime;

import static br.com.mmgabri.domain.enuns.ComandoContaStatusEnum.PENDING;

/**
 * Trilha de auditoria da efetivação (PENDING → COMPLETED → COMPLETED_ACK).
 * Sem compare-and-swap: nesse desenho as 3 escritas são sempre feitas por
 * este mesmo serviço, em ordem causal (o passo seguinte só existe porque o
 * anterior aconteceu) — não há dois processos disputando a mesma transição
 * como havia no desenho anterior (autorizador vs. ledger).
 */
@Repository
@RequiredArgsConstructor
public class ComandoContaRepository {
    private static final Logger logger = LoggerFactory.getLogger(ComandoContaRepository.class);

    private final DynamoDbTable<ComandoContaEntity> table;
    private final MetricsService metricsService;

    /**
     * Cria o registro inicial (INSERT → PENDING).
     */
    public void insertPending(ComandoContaEntity entity) {
        var startTime = OffsetDateTime.now();
        entity.setStatus(PENDING);

        try {
            table.putItem(PutItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(entity)
                    .build());
            calculateLatency(startTime, "insertPending", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:error");
            logger.error("Failed to insert comando_conta into DynamoDB table. correlationId={}", entity.getCorrelationId(), e);
        }
    }

    /**
     * Grava o resultado real (approved/errorCode/errorDescription) e marca
     * COMPLETED — chamado pelo passo PUB, antes do publish no Redis.
     */
    public void updateCompleted(ComandoContaEntity fields) {
        update(fields, ComandoContaStatusEnum.COMPLETED, "updateCompleted");
    }

    /**
     * Marca COMPLETED_ACK — chamado pelo passo SUB, depois de consumir a
     * notificação via pub/sub (com ou sem future pendente ainda vivo).
     */
    public void updateCompletedAck(String correlationId) {
        ComandoContaEntity fields = ComandoContaEntity.builder()
                .correlationId(correlationId)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
        update(fields, ComandoContaStatusEnum.COMPLETED_ACK, "updateCompletedAck");
    }

    private void update(ComandoContaEntity fields, ComandoContaStatusEnum to, String method) {
        var startTime = OffsetDateTime.now();
        fields.setStatus(to);
        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(fields)
                    .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                    .build());
            calculateLatency(startTime, method, fields.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:" + method, "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:" + method, "status:error");
            logger.error("Failed to apply {} on comando_conta. correlationId={}", method, fields.getCorrelationId(), e);
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("Latência dynamodb (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
