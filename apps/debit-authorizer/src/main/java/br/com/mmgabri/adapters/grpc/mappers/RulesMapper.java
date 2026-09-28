package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesRequest;
import org.springframework.stereotype.Component;

@Component
public class RulesMapper {

    public CheckRulesRequest payloadToCheckRulesRequest(Payload payload) {

        return CheckRulesRequest.newBuilder()
                .putAllMessageIso(payload.getMessageIso())
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnRules())
                .setSleep(payload.getExecutionSimulationConfig().getSleepRules())
                .build();
    }
}
