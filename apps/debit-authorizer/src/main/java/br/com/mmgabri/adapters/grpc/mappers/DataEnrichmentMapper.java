package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichTransactionRequest;
import org.springframework.stereotype.Component;

@Component
public class DataEnrichmentMapper {

    public EnrichTransactionRequest payloadToEnrichTransactionRequest(Payload payload) {

        return EnrichTransactionRequest.newBuilder()
                .setCardNumber(payload.getMessageIso().get("002"))
                .setSleep(payload.getExecutionSimulationConfig().getSleepEnrichment())
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnEnrichment())
                .build();
    }
}
