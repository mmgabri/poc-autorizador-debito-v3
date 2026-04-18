package br.com.mmgabri.services;

import br.com.mmgabri.grpc.LedgerRequest;
import br.com.mmgabri.grpc.LedgerResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LedgerSimulacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerSimulacaoService.class);

    @SneakyThrows
    public LedgerResponse execute(LedgerRequest request){
        String correlationId = request.getHeaderMessageGrpc().getCorrelationId();
        logger.debug("Simulação iniciada. correlationId={}", correlationId);

        int sleep = request.getSleepLedgerSimulacao();
        if (sleep > 0) {
            TimeUnit.MILLISECONDS.sleep(sleep);
        }

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                .setCorrelationId(correlationId)
                .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                .setMessage(request.getHeaderMessageGrpc().getMessage())
                .build();

        logger.debug("Simulação concluída sem chamar conta. correlationId={}", correlationId);

        return LedgerResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setAprovado(true)
                .setContaId(request.getContaId())
                .build();
    }
}