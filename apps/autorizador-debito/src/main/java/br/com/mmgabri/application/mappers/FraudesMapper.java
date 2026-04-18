package br.com.mmgabri.application.mappers;

import br.com.mmgabri.application.domains.FraudesRequest;
import br.com.mmgabri.application.domains.Payload;
import org.springframework.stereotype.Service;

@Service
public class FraudesMapper {

    public FraudesRequest mapToFraudesRequest(Payload payload){
        FraudesRequest request = FraudesRequest.builder()
                .customReturnFraude(payload.getExecutionSimulationConfig().getCustomReturnFraude())
                .sleepFraude(payload.getExecutionSimulationConfig().getSleepFraude())
                .contaId(payload.getDataEnrichment().getConta().getContaId())
                .valor(payload.getMessageIso().get("004"))
                .productName(payload.getProductDomain().getProductName())
                .build();
        return request;
    }
}
