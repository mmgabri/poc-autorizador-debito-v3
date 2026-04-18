package br.com.mmgabri.application.domains;

import br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionExecutionContext {
    private AuthorizationStatusEnum status;
    private String transactionId;
    private Payload payload;
    private String createdAt;
    private String reversedAt;
    private List<ServiceExecutionContext> services;
}
