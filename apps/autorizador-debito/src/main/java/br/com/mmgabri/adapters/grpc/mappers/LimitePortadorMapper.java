package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.LimitePortadorRequest;
import br.com.mmgabri.grpc.LimitePortadorRequest;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class LimitePortadorMapper {

    public LimitePortadorRequest payloadToLimitePortadorRequest(Payload payload,boolean isReversal ) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .setIsReversal(isReversal)
                .build();

        return LimitePortadorRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(payload.getDataEnrichment().getConta().getContaId())
                .setValor(payload.getMessageIso().get("004"))
                .setType("D")
                .setCustomReturnLimitePortador(payload.getExecutionSimulationConfig().getCustomReturnLimitePortador())
                .setSleepLimitePortador(payload.getExecutionSimulationConfig().getSleepLimitePortador())
                .build();
    }
}
