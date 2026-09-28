package br.com.mmgabri.adapters.dynamodb.entity;

import br.com.mmgabri.domain.enums.AccountCommandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamoDbBean
public class AccountCommandEntity {
    public static final String TABLE_NAME = "comando_conta";

    private String correlationId;
    private AccountCommandStatus status;
    private String accountId;
    private Boolean approved;
    private String errorCode;
    private String errorDescription;
    private String updatedAt;

    @DynamoDbPartitionKey
    public String getCorrelationId() {
        return correlationId;
    }

    // Stored attribute name kept as-is: existing items in the table use it.
    @DynamoDbAttribute("contaId")
    public String getAccountId() {
        return accountId;
    }
}
