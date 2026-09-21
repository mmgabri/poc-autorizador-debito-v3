package br.com.mmgabri.adapters.dynamodb.repository;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.application.domains.enuns.ComandoContaStatusEnum;
import br.com.mmgabri.application.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.model.IgnoreNullsMode;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.ReturnValuesOnConditionCheckFailure;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ComandoContaRepository {
    private static final Logger logger = LoggerFactory.getLogger(ComandoContaRepository.class);

    private final DynamoDbTable<ComandoContaEntity> table;
    private final MetricsService metricsService;

    /**
     * Cria o registro inicial (INSERT → PENDING), condicionado a não existir
     * ainda — protege contra sobrescrever um registro em andamento caso o
     * despacho seja disparado mais de uma vez para o mesmo correlationId.
     */
    public boolean insertPending(ComandoContaEntity entity) {
        var startTime = OffsetDateTime.now();
        entity.setStatus(ComandoContaStatusEnum.PENDING);

        Expression condition = Expression.builder()
                .expression("attribute_not_exists(correlationId)")
                .build();

        try {
            table.putItem(PutItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(entity)
                    .conditionExpression(condition)
                    .build());
            calculateLatency(startTime, "insertPending", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:success");
            return true;
        } catch (ConditionalCheckFailedException e) {
            logger.warn("Comando de efetivação já registrado - possível redespacho. correlationId={}", entity.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:already_exists");
            return false;
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:insertPending", "status:error");
            logger.error("Failed to insert comando_conta into DynamoDB table. correlationId={}", entity.getCorrelationId(), e);
            return false;
        }
    }

    /**
     * Tenta a transição {@code from} → {@code to} de forma atômica
     * (compare-and-swap): só grava {@code fields} (com status já setado para
     * {@code to}) se o status atual no DynamoDB for exatamente {@code from}.
     * <p>
     * Retorna o item completo pós-escrita (o Enhanced Client já devolve
     * {@code ReturnValue.ALL_NEW} de graça em todo UpdateItem, sem custo
     * adicional) — evita uma leitura separada só pra pegar os campos que o
     * ledger já tinha gravado antes (approved, errorCode, errorDescription).
     * <p>
     * Quando a condição não bate — o que é esperado sempre que o outro lado
     * (ledger) já mudou o status antes, não é um erro — retorna mesmo assim a
     * última imagem do item que está na tabela (via {@code
     * ReturnValuesOnConditionCheckFailure.ALL_OLD}, devolvida dentro da
     * própria {@link ConditionalCheckFailedException}), para que quem chama
     * possa decidir o que fazer a seguir sem precisar de uma leitura extra.
     */
    public Optional<ComandoContaEntity> tryUpdateTransition(ComandoContaEntity fields, ComandoContaStatusEnum from, ComandoContaStatusEnum to) {
        var startTime = OffsetDateTime.now();
        fields.setStatus(to);

        Expression condition = Expression.builder()
                .expression("#status = :from")
                .putExpressionName("#status", "status")
                .putExpressionValue(":from", AttributeValue.builder().s(from.name()).build())
                .build();

        try {
            ComandoContaEntity updated = table.updateItem(UpdateItemEnhancedRequest.builder(ComandoContaEntity.class)
                    .item(fields)
                    .ignoreNullsMode(IgnoreNullsMode.SCALAR_ONLY)
                    .conditionExpression(condition)
                    .returnValuesOnConditionCheckFailure(ReturnValuesOnConditionCheckFailure.ALL_OLD)
                    .build());
            calculateLatency(startTime, from + "->" + to, fields.getCorrelationId());
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryUpdateTransition", "from:" + from, "to:" + to, "status:success");
            return Optional.of(updated);
        } catch (ConditionalCheckFailedException e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryUpdateTransition", "from:" + from, "to:" + to, "status:condition_failed");
            if (e.hasItem() && !e.item().isEmpty()) {
                return Optional.of(table.tableSchema().mapToItem(e.item()));
            }
            return Optional.empty();
        } catch (Exception e) {
            metricsService.incrementMetric("app_duration_dynamodb", startTime, "table:comando_conta", "method:tryUpdateTransition", "from:" + from, "to:" + to, "status:error");
            logger.error("Failed to apply transition {}->{} on comando_conta. correlationId={}", from, to, fields.getCorrelationId(), e);
            throw e;
        }
    }

    private void calculateLatency(OffsetDateTime startTime, String operation, String correlationId) {
        long delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        logger.debug("Latência dynamodb (ms) {} correlationId={}: {}", operation, correlationId, delay);
    }
}
