package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.AccountCommandEntity;
import br.com.mmgabri.domain.enums.AccountCommandStatus;
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

import static br.com.mmgabri.domain.enums.AccountCommandStatus.PENDING;

/**
 * Audit trail of the commit (PENDING → COMPLETED → COMPLETED_ACK).
 * No compare-and-swap: in this design the 3 writes are always made by this same
 * service, in causal order (each step only exists because the previous one
 * happened) - there are no two processes contending for the same transition, as
 * there were in the previous design (authorizer vs. ledger).
 */
@Repository
@RequiredArgsConstructor
public class AccountCommandRepository {
    private static final Logger logger = LoggerFactory.getLogger(AccountCommandRepository.class);

    private final DynamoDbTable<AccountCommandEntity> table;
    private final MetricsService metricsService;

    /**
     * Creates the initial record (INSERT → PENDING).
     */
    public void insertPending(AccountCommandEntity entity) {
        var startTime = OffsetDateTime.now();
        entity.setStatus(PENDING);

        try {
            table.putItem(PutItemEnhancedRequest.builder(AccountCommandEntity.class)
                    .item(entity)
                    .build());
            calculateLatency(startTime, "insertPending", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:error");
            logger.error("Failed to insert account command into DynamoDB. correlationId={}", entity.getCorrelationId(), e);
        }
    }

    /**
     * Stores the actual result (approved/errorCode/errorDescription) and marks it
     * COMPLETED - called by the PUB step, before publishing to Redis.
     */
    public void updateCompleted(AccountCommandEntity fields) {
        update(fields, AccountCommandStatus.COMPLETED, "updateCompleted");
    }

    /**
     * Marks COMPLETED_ACK - called by the SUB step, after consuming the
     * notification via pub/sub (whether or not the pending future is still alive).
     */
    public void updateCompletedAck(String correlationId) {
        AccountCommandEntity fields = AccountCommandEntity.builder()
                .correlationId(correlationId)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
        update(fields, AccountCommandStatus.COMPLETED_ACK, "updateCompletedAck");
    }

    private void update(AccountCommandEntity fields, AccountCommandStatus to, String method) {
        var startTime = OffsetDateTime.now();
        fields.setStatus(to);
        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(AccountCommandEntity.class)
                    .item(fields)
                    .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                    .build());
            calculateLatency(startTime, method, fields.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:" + method, "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:" + method, "status:error");
            logger.error("Failed to apply {} on account command. correlationId={}", method, fields.getCorrelationId(), e);
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("DynamoDB latency (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
