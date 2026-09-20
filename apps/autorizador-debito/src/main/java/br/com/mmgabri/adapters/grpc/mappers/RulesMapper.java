package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.rules.v1.RulesRequest;
import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class RulesMapper {

    public RulesRequest payloadToRulesRequest(Payload payload) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .build();

        return RulesRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .putAllMessageIso(payload.getMessageIso())
                .setCustomReturnRules(payload.getExecutionSimulationConfig().getCustomReturnRules())
                .setSleepRules(payload.getExecutionSimulationConfig().getSleepRules())
                .build();
    }
}
