package br.com.mmgabri.application.domains;

import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceExecutionContext {
    private String transactionId;
    private ServicesEnum service;
    private AuthorizationStatusEnum status;
    private String errorCode;
    private String errorDescription;
    private String executedAt;
    private String reversedAt;
}
