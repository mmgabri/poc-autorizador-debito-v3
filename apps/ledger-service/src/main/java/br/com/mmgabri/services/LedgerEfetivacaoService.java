package br.com.mmgabri.services;

import br.com.mmgabri.adapters.sqs.ComandoContaSqsPublisher;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.RedisCallbackMessage;
import br.com.mmgabri.grpc.LedgerRequest;
import br.com.mmgabri.grpc.LedgerResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class LedgerEfetivacaoService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerEfetivacaoService.class);

    private final MetricsService metricsService;
    private final ComandoContaSqsPublisher sqsPublisher;
    private final PendingRequestService pendingRequestService;
    private final String instanceId;

    public LedgerEfetivacaoService(ComandoContaSqsPublisher sqsPublisher,
                                   PendingRequestService pendingRequestService,
                                   @Qualifier("instanceId") String instanceId,
                                   MetricsService metricsService) {
        this.sqsPublisher = sqsPublisher;
        this.pendingRequestService = pendingRequestService;
        this.instanceId = instanceId;
        this.metricsService = metricsService;
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

        // Register future BEFORE publishing to SQS to avoid race condition:
        // if conta-service processes and calls back before the future is registered,
        // the callback would be lost and waitForCallback would timeout.
        var future = pendingRequestService.register(correlationId);
        sqsPublisher.publish(comando);

        var startTime = OffsetDateTime.now();

        RedisCallbackMessage callback = pendingRequestService.waitForCallback(correlationId, future);

        metricsService.incrementMetric("app_ledger_duration_call_async", startTime, "tipo_operacao:EFETIVACAO");

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
                .setApproved(callback.approved())
                .setErrorCode(callback.errorCode())
                .setErrorDescription(callback.errorDescription())
                .setContaId(request.getContaId())
                .build();
    }
}