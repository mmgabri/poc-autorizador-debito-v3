package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {

    public RequestPostingRequest payloadToRequestPostingRequest(Payload payload, String operationType) {

        boolean isSimulation = "SIMULATION".equalsIgnoreCase(operationType);
        int sleep = isSimulation
                ? payload.getExecutionSimulationConfig().getSleepAccountPostingSimulation()
                : payload.getExecutionSimulationConfig().getSleepAccountPostingCommit();

        return RequestPostingRequest.newBuilder()
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setAccountId(payload.getDataEnrichment().getAccount().getAccountId())
                .setAmount(payload.getMessageIso().get("004"))
                .setDescription(payload.getMessageIso().getOrDefault("043", ""))
                .setAccountingScheme(payload.getProductDomain().getAccountingScheme())
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnAccountPosting())
                .setSleepCommit(isSimulation ? 0 : sleep)
                .setSleepSimulation(isSimulation ? sleep : 0)
                .setOperationType(operationType)
                .build();
    }
}
