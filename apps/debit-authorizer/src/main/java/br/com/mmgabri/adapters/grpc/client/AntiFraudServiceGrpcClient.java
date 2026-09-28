package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.AntiFraudGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.AntiFraudMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.ANTIFRAUD_SERVICE;

@Service
@RequiredArgsConstructor
public class AntiFraudServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(AntiFraudServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final AntiFraudGrpcStubProvider antiFraudGrpcStubProvider;
    private final AntiFraudMapper mapper;

    @SneakyThrows
    public AnalyzeFraudResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToAnalyzeFraudRequest(payload);
            logger.debug("Starting service call: {}", ANTIFRAUD_SERVICE.getServiceName());
            var stub = antiFraudGrpcStubProvider.getStub(payload.getHeaderMessage());
            var response = stub.analyzeFraud(request);
            return handleResponse(response, startTime);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(ANTIFRAUD_SERVICE, e);
            logger.error("Service {} execution failed [{}]: {}", ANTIFRAUD_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private AnalyzeFraudResponse handleResponse(AnalyzeFraudResponse response, OffsetDateTime startTime) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(ANTIFRAUD_SERVICE, result);
            logger.error("Transaction denied by service '{}': {} - {}", ANTIFRAUD_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", ANTIFRAUD_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
