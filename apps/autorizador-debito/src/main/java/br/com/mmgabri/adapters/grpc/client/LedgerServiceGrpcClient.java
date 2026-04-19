package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LedgerGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LedgerMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.LedgerResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LEDGER_SERVICE;

@Service
@RequiredArgsConstructor
public class LedgerServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final LedgerGrpcStubProvider ledgerGrpcStubProvider;
    private final LedgerMapper mapper;

    @SneakyThrows
    public LedgerResponse execute(Payload payload, String tipoOperacao) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLedgerRequest(payload, tipoOperacao);
            logger.debug("Starting service call: {} - tipoOperacao: {}", LEDGER_SERVICE.getServiceName(), tipoOperacao);
            var response = ledgerGrpcStubProvider.getStub().gerarLancamento(request);
            return handleResponse(response, startTime, tipoOperacao);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LEDGER_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(LEDGER_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private LedgerResponse handleResponse(LedgerResponse response, OffsetDateTime startTime, String tipoOperacao) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}' [{}]: {} - {}", LEDGER_SERVICE.getServiceName(), tipoOperacao, response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(LEDGER_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} [{}] executed successfully", LEDGER_SERVICE.getServiceName(), tipoOperacao);
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
