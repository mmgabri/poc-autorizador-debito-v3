package br.com.mmgabri.services;

import br.com.mmgabri.adapters.sqs.ComandoContaSqsPublisher;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.RedisCallbackMessage;
import br.com.mmgabri.grpc.LedgerRequest;
import br.com.mmgabri.grpc.LedgerResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class LedgerEfetivacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEfetivacaoService.class);

    private final ComandoContaSqsPublisher sqsPublisher;
    private final PendingRequestService pendingRequestService;
    private final String instanceId;

    public LedgerEfetivacaoService(ComandoContaSqsPublisher sqsPublisher,
                                   PendingRequestService pendingRequestService,
                                   @Qualifier("instanceId") String instanceId) {
        this.sqsPublisher = sqsPublisher;
        this.pendingRequestService = pendingRequestService;
        this.instanceId = instanceId;
    }

    public LedgerResponse execute(LedgerRequest request) throws Exception {
        String correlationId = request.getHeaderMessageGrpc().getCorrelationId();
        logger.debug("Efetivação iniciada. correlationId={}", correlationId);

        ComandoContaRequest comando = new ComandoContaRequest(
                correlationId,
                instanceId,
                request.getCustomReturnLedger(),
                request.getSleepLedgerEfetivacao()
        );
        sqsPublisher.publish(comando);

        RedisCallbackMessage callback = pendingRequestService.waitForCallback(correlationId);

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                .setCorrelationId(correlationId)
                .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                .setMessage(request.getHeaderMessageGrpc().getMessage())
                .build();

        logger.debug("Efetivação concluída. correlationId={}", correlationId);

        return LedgerResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setAprovado(callback.aprovado())
                .setErrorCode(callback.errorCode())
                .setErrorDescription(callback.errorDescription())
                .setContaId(request.getContaId())
                .build();
    }
}