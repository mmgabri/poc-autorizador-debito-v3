package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.application.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComandoContaRepository {
    private static final Logger logger = LoggerFactory.getLogger(ComandoContaRepository.class);

    private final DynamoDbTable<ComandoContaEntity> table;
    private final MetricsService metricsService;

    public void save(ComandoContaEntity entity) {
        var startTime = OffsetDateTime.now();
        try {
            table.putItem(entity);
            calculateLatency(startTime, "save", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:save", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:save", "status:error");
            logger.error("Failed to save comando_conta into DynamoDB table. correlationId={}", entity.getCorrelationId(), e);
        }
    }

    public Optional<ComandoContaEntity> findByCorrelationId(String correlationId) {
        var startTime = OffsetDateTime.now();
        try {
            ComandoContaEntity entity = table.getItem(Key.builder().partitionValue(correlationId).build());
            calculateLatency(startTime, "get", correlationId);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:get", "status:success");
            return Optional.ofNullable(entity);
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:get", "status:error");
            logger.error("Failed to read comando_conta from DynamoDB table. correlationId={}", correlationId, e);
            return Optional.empty();
        }
    }

    /**
     * Transição atômica PENDING → TIMEOUT, condicionada ao status ainda ser PENDING.
     * Retorna false quando perde a corrida (o ledger já tinha marcado "completed"
     * antes) — nesse caso quem chamou deve ler o registro e usar o resultado real
     * em vez de tratar como timeout.
     */
    public boolean markTimeoutIfPending(String correlationId) {
        var startTime = OffsetDateTime.now();
        ComandoContaEntity entity = new ComandoContaEntity();
        entity.setCorrelationId(correlationId);
        entity.setStatus("TIMEOUT");
        entity.setUpdatedAt(OffsetDateTime.now().toString());

        Expression condition = Expression.builder()
                .expression("attribute_not_exists(#status) OR #status = :pending")
                .putExpressionName("#status", "status")
                .putExpressionValue(":pending", AttributeValue.builder().s("PENDING").build())
                .build();

        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(entity)
                    .ignoreNullsMode(IgnoreNullsMode.DEFAULT)
                    .conditionExpression(condition)
                    .build());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:markTimeout", "status:success");
            return true;
        } catch (ConditionalCheckFailedException e) {
            logger.debug("Não marcou TIMEOUT - ledger já tinha concluído antes. correlationId={}", correlationId);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:markTimeout", "status:lost_race");
            return false;
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:markTimeout", "status:error");
            logger.error("Failed to mark comando_conta as TIMEOUT. correlationId={}", correlationId, e);
            return false;
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("Latência dynamodb (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
