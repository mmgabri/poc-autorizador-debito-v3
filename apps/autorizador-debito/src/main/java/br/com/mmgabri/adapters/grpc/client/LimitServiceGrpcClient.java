package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LimiteGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LimiteMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.grpc.LimiteResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LIMIT_SERVICE;

@Service
@RequiredArgsConstructor
public class LimitServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LimitServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final LimiteGrpcStubProvider limiteGrpcClient;
    private final LimiteMapper mapper;

    @SneakyThrows
    public LimiteResponse execute(Payload payload, String tipoOperacao) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLimiteRequest(payload, tipoOperacao);
            logger.debug("Starting service call: {} - tipoOperacao: {}", LIMIT_SERVICE.getServiceName(), tipoOperacao);
            var response = limiteGrpcClient.getStub().atualizarLimite(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LIMIT_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ LIMIT_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(LIMIT_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private LimiteResponse handleResponse(LimiteResponse response, OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", LIMIT_SERVICE.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ LIMIT_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(LIMIT_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", LIMIT_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ LIMIT_SERVICE.getServiceName(), "status:success");
        return response;
    }
}