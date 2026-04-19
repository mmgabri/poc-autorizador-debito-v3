package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ServiceContextEntity;
import br.com.mmgabri.application.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ServiceContextRepository {
    private static final Logger logger = LoggerFactory.getLogger(ServiceContextRepository.class);

    private final DynamoDbTable<ServiceContextEntity> table;
    private final MetricsService metricsService;

    public void save(ServiceContextEntity service) {
        var startTime = OffsetDateTime.now();
        try {
            table.putItem(service);
            calculateLatency(startTime, "save", service.getTransactionId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:service_calls", "method:save", "status:success");
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:service_calls", "method:save", "status:error");
            logger.error("Failed to insert service into DynamoDB table.", e);
        }
    }

    public List<ServiceContextEntity> getServicesByTransactionId(String transactionId) {
        var startTime = OffsetDateTime.now();
        try {
            var results = table.query(r -> r
                    .queryConditional(QueryConditional.keyEqualTo(k -> k.partitionValue(transactionId)))
                    .consistentRead(true)
            );

            List<ServiceContextEntity> items = results.items().stream().toList();
            calculateLatency(startTime, "query", transactionId);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:service_calls", "method:getServicesByTransactionId", "status:success");
            return items;

        } catch (Exception e) {
            logger.error("Failed to query services from DynamoDB table. transactionId={}", transactionId, e);
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:service_calls", "method:getServicesByTransactionId", "status:error");
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