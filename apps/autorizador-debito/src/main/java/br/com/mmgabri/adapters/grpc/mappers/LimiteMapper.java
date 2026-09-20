package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.limit.v1.LimiteRequest;
import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class LimiteMapper {

    public LimiteRequest payloadToLimiteRequest(Payload payload, String tipoOperacao) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .build();

        return LimiteRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(payload.getDataEnrichment().getConta().getContaId())
                .setValor(payload.getMessageIso().get("004"))
                .setCustomReturnLimit(payload.getExecutionSimulationConfig().getCustomReturnLimit())
                .setSleepLimitEfetivacao(payload.getExecutionSimulationConfig().getSleepLimitEfetivacao())
                .setSleepLimitSimulacao(payload.getExecutionSimulationConfig().getSleepLimitSimulacao())
                .setTipoOperacao(tipoOperacao)
                .build();
    }
}
