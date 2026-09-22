package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LedgerGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LedgerMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.grpc.ledger.v1.LedgerResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LEDGER_SERVICE;
import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LEDGER_SERVICE_SIMULATION;

@Service
@RequiredArgsConstructor
public class LedgerServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final LedgerGrpcStubProvider ledgerGrpcStubProvider;
    private final LedgerMapper mapper;

    @SneakyThrows
    public LedgerResponse execute(Payload payload, String tipoOperacao) {
        ServicesEnum service = "SIMULACAO".equals(tipoOperacao) ? LEDGER_SERVICE_SIMULATION : LEDGER_SERVICE;
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLedgerRequest(payload, tipoOperacao);
            logger.debug("Starting service call: {} - tipoOperacao: {}", service.getServiceName(), tipoOperacao);

            var response = ledgerGrpcStubProvider.getStub().gerarLancamento(request);
            return handleResponse(response, startTime, service);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", service.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:error");
            throw new BusinessException(service.getServiceName(), "999", e.getMessage());
        }
    }

    private LedgerResponse handleResponse(LedgerResponse response, OffsetDateTime startTime, ServicesEnum service) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", service.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:error_business");
            throw new BusinessException(service.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", service.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:success");
        return response;
    }
}
