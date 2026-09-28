package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LedgerGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LedgerMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.domains.enums.ServicesEnum;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.LEDGER_SERVICE;
import static br.com.mmgabri.application.domains.enums.ServicesEnum.LEDGER_SERVICE_SIMULATION;

@Service
@RequiredArgsConstructor
public class LedgerServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final LedgerGrpcStubProvider ledgerGrpcStubProvider;
    private final LedgerMapper mapper;

    @SneakyThrows
    public RequestPostingResponse execute(Payload payload, String operationType) {
        ServicesEnum service = "SIMULATION".equals(operationType) ? LEDGER_SERVICE_SIMULATION : LEDGER_SERVICE;
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToRequestPostingRequest(payload, operationType);
            logger.debug("Starting service call: {} - operationType: {}", service.getServiceName(), operationType);

            var stub = ledgerGrpcStubProvider.getStub(payload.getHeaderMessage());
            var response = stub.requestPosting(request);
            return handleResponse(response, startTime, service);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(service, e);
            logger.error("Service {} execution failed [{}]: {}", service.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private RequestPostingResponse handleResponse(RequestPostingResponse response, OffsetDateTime startTime, ServicesEnum service) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(service, result);
            logger.error("Transaction denied by service '{}': {} - {}", service.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", service.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + service.getServiceName(), "status:success");
        return response;
    }
}
