package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.SegurancaGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.SegurancaMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.security.v1.SegurancaResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class SecurityServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(SecurityServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final SegurancaGrpcStubProvider segurancaGrpcClient;
    private final SegurancaMapper mapper;

    @SneakyThrows
    public SegurancaResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToSegurancaRequest(payload);
            logger.debug("Starting service call: {}" , SECURITY_SERVICE.getServiceName());
            var response =  segurancaGrpcClient.getStub().validarSeguranca(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", SECURITY_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ SECURITY_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(SECURITY_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private SegurancaResponse handleResponse(SegurancaResponse response,  OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", SECURITY_SERVICE.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ SECURITY_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(SECURITY_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", SECURITY_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ SECURITY_SERVICE.getServiceName(), "status:success");
        return response;
    }
}