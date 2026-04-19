package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.LedgerRequest;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.springframework.stereotype.Component;

@Component
public class LedgerMapper {

    public LedgerRequest payloadToLedgerRequest(Payload payload, String tipoOperacao) {

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(payload.getHeaderMessage().getMessage())
                .build();

        boolean isSimulacao = "SIMULACAO".equalsIgnoreCase(tipoOperacao);
        int sleep = isSimulacao
                ? payload.getExecutionSimulationConfig().getSleepLedgerSimulacao()
                : payload.getExecutionSimulationConfig().getSleepLedgerEfetivacao();

        return LedgerRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .setContaId(payload.getDataEnrichment().getConta().getContaId())
                .setValor(payload.getMessageIso().get("004"))
                .setLiteral(payload.getMessageIso().getOrDefault("043", ""))
                .setRoteiroContabil(payload.getProductDomain().getRoteiroContabil())
                .setCustomReturnLedger(payload.getExecutionSimulationConfig().getCustomReturnLedger())
                .setSleepLedgerEfetivacao(isSimulacao ? 0 : sleep)
                .setSleepLedgerSimulacao(isSimulacao ? sleep : 0)
                .setTipoOperacao(tipoOperacao)
                .build();
    }
}
