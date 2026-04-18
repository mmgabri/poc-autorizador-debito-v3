package br.com.mmgabri.adapters.database.dynamodb.repository;

import br.com.mmgabri.adapters.database.dynamodb.entity.IdempotencyEntity;
import br.com.mmgabri.application.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotencyRepository {
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyRepository.class);

    private final DynamoDbTable<IdempotencyEntity> table;
    private final MetricsService metricsService;

    public void save(IdempotencyEntity idempotency) {
        var startTime = OffsetDateTime.now();
        try {
            table.putItem(idempotency);
            calculateLatency(startTime, "save", idempotency.getTransactionId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:idempotency", "method:save", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:idempotency", "method:save", "status:error");
            logger.error("Failed to insert idempotency into DynamoDB table.", e);
        }
    }

    public Optional<IdempotencyEntity> getByCorrelationId(String correlationId) {
        var startTime = OffsetDateTime.now();
        try {
            IdempotencyEntity idempotency = table.getItem(r -> r
                    .key(k -> k.partitionValue(correlationId))
                    .consistentRead(true));
            calculateLatency(startTime, "get", correlationId);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:idempotency", "method:getTransactionById", "status:success");
            if (idempotency == null){
                return Optional.empty();
            }
            return Optional.of(idempotency);
        } catch (Exception e) {
            logger.error("Failed to insert idempotency into DynamoDB table.", e);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:idempotency", "method:getTransactionById", "status:error");
            throw e;
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        var delay = 0L;
        delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("{}Latência dynamodb (ms) {}: {}", correlationId, operation, delay);
    }
}