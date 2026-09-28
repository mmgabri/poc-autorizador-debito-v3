package br.com.mmgabri.adapters.dynamodb.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class IdempotencyEntity {
    public static final String TABLE_NAME = "idempotency";
    private String correlationId;
    private String transactionId;
    private String executedAt;

    @DynamoDbPartitionKey
    public String getCorrelationId() {
        return correlationId;
    }
}