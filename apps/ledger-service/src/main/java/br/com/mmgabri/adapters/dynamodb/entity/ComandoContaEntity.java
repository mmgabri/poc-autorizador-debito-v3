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
public class ComandoContaEntity {
    public static final String TABLE_NAME = "comando_conta";

    private String correlationId;
    private String status;
    private String contaId;
    private Boolean approved;
    private String errorCode;
    private String errorDescription;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getCorrelationId() {
        return correlationId;
    }
}
