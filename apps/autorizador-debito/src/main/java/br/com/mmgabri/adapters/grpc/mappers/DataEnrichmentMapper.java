package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.EnrichByCardRequest;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class DataEnrichmentMapper {

    public EnrichByCardRequest payloadToEnrichByCardRequestRequest(Payload payload) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .build();

        return EnrichByCardRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setNumeroCartao(payload.getMessageIso().get("002"))
                .setSleepDataEnrichment(payload.getExecutionSimulationConfig().getSleepDataEnrichment())
                .setCustomReturnDataEnrichment(payload.getExecutionSimulationConfig().getCustomReturnDataEnrichment())
                .build();
    }
}
