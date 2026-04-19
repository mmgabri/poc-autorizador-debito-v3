package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.TransactionContextEntity;
import br.com.mmgabri.application.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

import java.time.Duration;
import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class TransactionContextRepository {
    private static final Logger logger = LoggerFactory.getLogger(TransactionContextRepository.class);

    private final DynamoDbTable<TransactionContextEntity> table;
    private final MetricsService metricsService;

    public void save(TransactionContextEntity transaction) {
        var startTime = OffsetDateTime.now();
        try {
            table.putItem(transaction);
            calculateLatency(startTime, "save", transaction.getTransactionId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:transaction", "method:save", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:transaction", "method:save", "status:error");
            logger.error("Failed to insert transaction into DynamoDB table.", e);
        }

    }

    public TransactionContextEntity getByTransactionId(String transactionId) {
        var startTime = OffsetDateTime.now();
        try {
            TransactionContextEntity transaction = table.getItem(r -> r
                    .key(k -> k.partitionValue(transactionId))
                    .consistentRead(true));
            calculateLatency(startTime, "get", transaction.getTransactionId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:transaction", "method:getTransactionById", "status:success");
            return transaction;
        } catch (Exception e) {
            logger.error("Failed to insert transaction into DynamoDB table.", e);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:transaction", "method:getTransactionById", "status:error");
            throw e;
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String transactionId) {
        var delay = 0L;
        delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        //     incrementMetric(metricsService, "app_duration_dynamodb", operation, startTime);
        logger.debug("{}Latência dynamodb (ms) {}: {}", transactionId, operation, delay);
    }
}