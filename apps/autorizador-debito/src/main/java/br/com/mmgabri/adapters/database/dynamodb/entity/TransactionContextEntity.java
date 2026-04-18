package br.com.mmgabri.adapters.database.dynamodb.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class TransactionContextEntity {
    public static final String TABLE_NAME = "transaction_context";
    private String transactionId;
    private String status;
    private String payload;
    private String createdAt;
    private String updatedAt;
    private String reversedAt;

    @DynamoDbPartitionKey
    public String getTransactionId() {
        return transactionId;
    }
}