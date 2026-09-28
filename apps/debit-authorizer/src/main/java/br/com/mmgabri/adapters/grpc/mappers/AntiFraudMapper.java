package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudRequest;
import org.springframework.stereotype.Component;

@Component
public class AntiFraudMapper {

    public AnalyzeFraudRequest payloadToAnalyzeFraudRequest(Payload payload) {

        return AnalyzeFraudRequest.newBuilder()
                .setAccountId(payload.getDataEnrichment().getAccount().getAccountId())
                .setAmount(payload.getMessageIso().get("004"))
                .setProductName(payload.getProductDomain().getProductName())
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnAntifraud())
                .setSleep(payload.getExecutionSimulationConfig().getSleepAntifraud())
                .build();
    }
}
