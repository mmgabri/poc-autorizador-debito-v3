package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.SegurancaRequest;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class SegurancaMapper {

    public SegurancaRequest payloadToSegurancaRequest(Payload payload,boolean isReversal ) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .setIsReversal(isReversal)
                .build();

        return SegurancaRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(payload.getDataEnrichment().getConta().getContaId())
                .setDadosChip(payload.getMessageIso().get("055"))
                .setSenha(payload.getMessageIso().get("052"))
                .setNumeroCartao(payload.getMessageIso().get("002"))
                .setCustomReturnSeguranca(payload.getExecutionSimulationConfig().getCustomReturnSeguranca())
                .setSleepSeguranca(payload.getExecutionSimulationConfig().getSleepSeguranca())
                .build();
    }
}
