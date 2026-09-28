package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.SecurityGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.SecurityMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class SecurityServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(SecurityServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final SecurityGrpcStubProvider securityGrpcClient;
    private final SecurityMapper mapper;

    @SneakyThrows
    public ValidateSecurityResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToValidateSecurityRequest(payload);
            logger.debug("Starting service call: {}" , SECURITY_SERVICE.getServiceName());
            var stub = securityGrpcClient.getStub(payload.getHeaderMessage());
            var response = stub.validateSecurity(request);
            return handleResponse(response, startTime);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(SECURITY_SERVICE, e);
            logger.error("Service {} execution failed [{}]: {}", SECURITY_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + SECURITY_SERVICE.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private ValidateSecurityResponse handleResponse(ValidateSecurityResponse response,  OffsetDateTime startTime) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(SECURITY_SERVICE, result);
            logger.error("Transaction denied by service '{}': {} - {}", SECURITY_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + SECURITY_SERVICE.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", SECURITY_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ SECURITY_SERVICE.getServiceName(), "status:success");
        return response;
    }
}