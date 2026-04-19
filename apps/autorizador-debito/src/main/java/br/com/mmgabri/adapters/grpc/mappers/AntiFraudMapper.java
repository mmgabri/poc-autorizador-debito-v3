package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.AntiFraudRequest;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class AntiFraudMapper {

    public AntiFraudRequest payloadToAntiFraudRequest(Payload payload) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .build();

        return AntiFraudRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(payload.getDataEnrichment().getConta().getContaId())
                .setValor(payload.getMessageIso().get("004"))
                .setProductName(payload.getProductDomain().getProductName())
                .setCustomReturnFraude(payload.getExecutionSimulationConfig().getCustomReturnFraude())
                .setSleepFraude(payload.getExecutionSimulationConfig().getSleepFraude())
                .build();
    }
}
