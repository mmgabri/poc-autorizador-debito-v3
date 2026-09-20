package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.RulesGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.RulesMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.rules.v1.RulesResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.RULES_SERVICE;

@Service
@RequiredArgsConstructor
public class RulesServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RulesServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final RulesGrpcStubProvider rulesGrpcStubProvider;
    private final RulesMapper mapper;

    @SneakyThrows
    public RulesResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToRulesRequest(payload);
            logger.debug("Starting service call: {}", RULES_SERVICE.getServiceName());
            var response = rulesGrpcStubProvider.getStub().validateRules(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", RULES_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(RULES_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private RulesResponse handleResponse(RulesResponse response, OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", RULES_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(RULES_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", RULES_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
