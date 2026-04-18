package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.SegurancaGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.SegurancaMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.SegurancaResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class SegurancaGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(SegurancaGrpcClient.class);

    private final MetricsService metricsService;
    private final SegurancaGrpcStubProvider segurancaGrpcClient;
    private final SegurancaMapper mapper;

    @SneakyThrows
    public SegurancaResponse execute(Payload payload, boolean isReversal) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToSegurancaRequest(payload, isReversal);
            logger.debug("Starting service call: {} - isReversal: {}" , SEGURANCA.getServiceName(), isReversal);
            var response =  segurancaGrpcClient.getStub().validarSeguranca(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", SEGURANCA.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+SEGURANCA.getServiceName(), "status:error");
            throw new BusinessException(SEGURANCA.getServiceName(), "999", e.getMessage());
        }
    }

    private SegurancaResponse handleResponse(SegurancaResponse response,  OffsetDateTime startTime) {
        if (!response.getAprovado()) {
            logger.error("Transaction denied by service '{}': {} - {}", SEGURANCA.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+SEGURANCA.getServiceName(), "status:error_business");
            throw new BusinessException(SEGURANCA.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", SEGURANCA.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+SEGURANCA.getServiceName(), "status:success");
        return response;
    }
}