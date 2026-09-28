package br.com.mmgabri.adapters.dynamodb.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@Getter
@Setter
@NoArgsConstructor
@DynamoDbBean
public class ServiceContextEntity {
    public static final String TABLE_NAME = "service_context";
    private String transactionId;
    private String serviceName;
    private String status;
    private String errorCode;
    private String errorDescription;
    private String executedAt;
    private String reversedAt;

    @DynamoDbPartitionKey
    public String getTransactionId() {
        return transactionId;
    }


    @DynamoDbSortKey
    public String getServiceName() {
        return serviceName;
    }
}