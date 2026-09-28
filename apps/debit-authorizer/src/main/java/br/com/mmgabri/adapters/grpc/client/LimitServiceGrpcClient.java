package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LimitGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LimitMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.LIMIT_SERVICE;

@Service
@RequiredArgsConstructor
public class LimitServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LimitServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final LimitGrpcStubProvider limitGrpcClient;
    private final LimitMapper mapper;

    @SneakyThrows
    public UpdateLimitResponse execute(Payload payload, String operationType) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToUpdateLimitRequest(payload, operationType);
            logger.debug("Starting service call: {} - operationType: {}", LIMIT_SERVICE.getServiceName(), operationType);
            var stub = limitGrpcClient.getStub(payload.getHeaderMessage());
            var response = stub.updateLimit(request);
            return handleResponse(response, startTime);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(LIMIT_SERVICE, e);
            logger.error("Service {} execution failed [{}]: {}", LIMIT_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LIMIT_SERVICE.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private UpdateLimitResponse handleResponse(UpdateLimitResponse response, OffsetDateTime startTime) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(LIMIT_SERVICE, result);
            logger.error("Transaction denied by service '{}': {} - {}", LIMIT_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LIMIT_SERVICE.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", LIMIT_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ LIMIT_SERVICE.getServiceName(), "status:success");
        return response;
    }
}