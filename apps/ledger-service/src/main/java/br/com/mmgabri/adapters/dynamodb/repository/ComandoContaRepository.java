package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Duration;
import java.time.OffsetDateTime;

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

    /**
     * Transição atômica PENDING → completed, condicionada ao status ainda ser
     * PENDING. Retorna false quando perde a corrida (o autorizador já tinha
     * desistido e marcado TIMEOUT) — quem chamou deve então gravar o resultado
     * real sem o sinal de Redis normal, já que ninguém está mais ouvindo.
     */
    public boolean completeIfPending(ComandoContaEntity entity) {
        var startTime = OffsetDateTime.now();
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
            calculateLatency(startTime, "completeIfPending", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:completeIfPending", "status:success");
            return true;
        } catch (ConditionalCheckFailedException e) {
            logger.debug("Não concluiu - autorizador já tinha marcado TIMEOUT antes. correlationId={}", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:completeIfPending", "status:lost_race");
            return false;
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:completeIfPending", "status:error");
            logger.error("Failed to complete comando_conta into DynamoDB table. correlationId={}", entity.getCorrelationId(), e);
            return false;
        }
    }

    /**
     * Grava o resultado real mesmo tendo perdido a corrida pro TIMEOUT do
     * autorizador — sem isso o resultado de negócio se perderia. Marca um status
     * distinto (COMPLETED_LATE) pra ferramentas de conciliação identificarem
     * exatamente esses casos depois.
     */
    public void forceCompleteLate(ComandoContaEntity entity) {
        var startTime = OffsetDateTime.now();
        entity.setStatus("COMPLETED_LATE");
        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(entity)
                    .ignoreNullsMode(IgnoreNullsMode.DEFAULT)
                    .build());
            calculateLatency(startTime, "forceCompleteLate", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:forceCompleteLate", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:forceCompleteLate", "status:error");
            logger.error("Failed to force-complete comando_conta into DynamoDB table. correlationId={}", entity.getCorrelationId(), e);
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("Latência dynamodb (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
