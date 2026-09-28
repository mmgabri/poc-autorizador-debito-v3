package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.RulesGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.RulesMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.RULES_SERVICE;

@Service
@RequiredArgsConstructor
public class RulesServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(RulesServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final RulesGrpcStubProvider rulesGrpcStubProvider;
    private final RulesMapper mapper;

    @SneakyThrows
    public CheckRulesResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToCheckRulesRequest(payload);
            logger.debug("Starting service call: {}", RULES_SERVICE.getServiceName());
            var stub = rulesGrpcStubProvider.getStub(payload.getHeaderMessage());
            var response = stub.checkRules(request);
            return handleResponse(response, startTime);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(RULES_SERVICE, e);
            logger.error("Service {} execution failed [{}]: {}", RULES_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private CheckRulesResponse handleResponse(CheckRulesResponse response, OffsetDateTime startTime) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(RULES_SERVICE, result);
            logger.error("Transaction denied by service '{}': {} - {}", RULES_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", RULES_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + RULES_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
