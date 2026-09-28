package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitRequest;
import org.springframework.stereotype.Component;

@Component
public class LimitMapper {

    public UpdateLimitRequest payloadToUpdateLimitRequest(Payload payload, String operationType) {

        return UpdateLimitRequest.newBuilder()
                .setAccountId(payload.getDataEnrichment().getAccount().getAccountId())
                .setAmount(payload.getMessageIso().get("004"))
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnLimit())
                .setSleepCommit(payload.getExecutionSimulationConfig().getSleepLimitCommit())
                .setSleepSimulation(payload.getExecutionSimulationConfig().getSleepLimitSimulation())
                .setOperationType(operationType)
                .build();
    }
}
