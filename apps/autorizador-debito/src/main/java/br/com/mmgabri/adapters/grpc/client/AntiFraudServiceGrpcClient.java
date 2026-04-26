package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.AntiFraudGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.AntiFraudMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.AntiFraudResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.ANTIFRAUD_SERVICE;

@Service
@RequiredArgsConstructor
public class AntiFraudServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(AntiFraudServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final AntiFraudGrpcStubProvider antiFraudGrpcStubProvider;
    private final AntiFraudMapper mapper;

    @SneakyThrows
    public AntiFraudResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToAntiFraudRequest(payload);
            logger.debug("Starting service call: {}", ANTIFRAUD_SERVICE.getServiceName());
            var response = antiFraudGrpcStubProvider.getStub().validarFraude(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", ANTIFRAUD_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(ANTIFRAUD_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private AntiFraudResponse handleResponse(AntiFraudResponse response, OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", ANTIFRAUD_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(ANTIFRAUD_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", ANTIFRAUD_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + ANTIFRAUD_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
