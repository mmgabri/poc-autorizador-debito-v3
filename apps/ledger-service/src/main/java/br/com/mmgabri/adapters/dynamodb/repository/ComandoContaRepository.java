package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.domain.enuns.ComandoContaStatusEnum;
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

    /**
     * Tenta a transição {@code from} → {@code to} de forma atômica
     * (compare-and-swap): só grava {@code fields} (com status já setado para
     * {@code to}) se o status atual no DynamoDB for exatamente {@code from}.
     * <p>
     * Retorna {@code false} sem lançar exceção quando a condição não bate —
     * isso é esperado sempre que o autorizador já mudou o status antes, não é
     * um erro. Quem chama decide o que fazer a seguir (ex: tentar outra
     * transição, ou desistir).
     */
    public boolean tryTransition(ComandoContaEntity fields, ComandoContaStatusEnum from, ComandoContaStatusEnum to) {
        var startTime = OffsetDateTime.now();
        fields.setStatus(to);

        Expression condition = Expression.builder()
                .expression("#status = :from")
                .putExpressionName("#status", "status")
                .putExpressionValue(":from", AttributeValue.builder().s(from.name()).build())
                .build();

        try {
            table.updateItem(UpdateItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(fields)
                    .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                    .conditionExpression(condition)
                    .build());
            calculateLatency(startTime, from + "->" + to, fields.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryTransition", "from:" + from, "to:" + to, "status:success");
            return true;
        } catch (ConditionalCheckFailedException e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryTransition", "from:" + from, "to:" + to, "status:condition_failed");
            return false;
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryTransition", "from:" + from, "to:" + to, "status:error");
            logger.error("Failed to apply transition {}->{} on comando_conta. correlationId={}", from, to, fields.getCorrelationId(), e);
            return false;
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("Latência dynamodb (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
