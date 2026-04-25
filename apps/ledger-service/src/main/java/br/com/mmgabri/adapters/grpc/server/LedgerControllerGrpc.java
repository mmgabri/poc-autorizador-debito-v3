package br.com.mmgabri.adapters.grpc.server;

import br.com.mmgabri.grpc.LedgerRequest;
import br.com.mmgabri.grpc.LedgerResponse;
import br.com.mmgabri.grpc.LedgerServiceGrpc;
import br.com.mmgabri.services.LedgerEfetivacaoService;
import br.com.mmgabri.services.LedgerSimulacaoService;
import br.com.mmgabri.services.MetricsService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class LedgerControllerGrpc extends LedgerServiceGrpc.LedgerServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(LedgerControllerGrpc.class);

    private final LedgerSimulacaoService ledgerSimulacaoService;
    private final LedgerEfetivacaoService ledgerEfetivacaoService;
    private final MetricsService metricsService;

    @Override
    public void gerarLancamento(LedgerRequest request, StreamObserver<LedgerResponse> responseObserver) {
        var startTime = OffsetDateTime.now();
        logger.debug("Received gerarLancamento. tipoOperacao={}", request.getTipoOperacao());
        try {
            var response = "SIMULACAO".equals(request.getTipoOperacao())
                    ? ledgerSimulacaoService.execute(request)
                    : ledgerEfetivacaoService.execute(request);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            onSuccess(startTime, request.getTipoOperacao());

        } catch (io.grpc.StatusRuntimeException e) {
            logger.warn("gRPC error on gerarLancamento: {}", e.getStatus(), e);
            responseObserver.onError(e);
        } catch (Exception e) {
            logger.error("Unexpected error on gerarLancamento", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Erro interno")
                            .asRuntimeException()
            );
        }
    }

    private void onSuccess(OffsetDateTime startTime, String tipoOperacao) {
        var delay = 0L;
        delay = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        metricsService.incrementMetric("app_ledger_duration_transaction", startTime, "method:gerarLancamento", "tipo_operacao:"+tipoOperacao);
        logger.debug("Processamento concluído em {} (ms)", delay);
    }
}
