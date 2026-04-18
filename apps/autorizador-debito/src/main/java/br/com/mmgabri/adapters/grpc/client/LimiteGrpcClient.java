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

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LIMITE;

@Service
@RequiredArgsConstructor
public class LimiteGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LimiteGrpcClient.class);

    private final MetricsService metricsService;
    private final LimiteGrpcStubProvider limiteGrpcClient;
    private final LimiteMapper mapper;
    private final TransactionContextRegistryService transactionRegistry;

    @SneakyThrows
    public LimiteResponse execute(Payload payload, boolean isReversal) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLimiteRequest(payload, isReversal);
            logger.debug("Starting service call: {} - isReversal: {}" , LIMITE.getServiceName(), isReversal);
            var response = limiteGrpcClient.getStub().atualizarLimite(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LIMITE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE.getServiceName(), "status:error");
            throw new BusinessException(LIMITE.getServiceName(), "999", e.getMessage());
        }
    }

    private LimiteResponse handleResponse(LimiteResponse response, OffsetDateTime startTime) {
        if (!response.getAprovado()) {
            logger.error("Transaction denied by service '{}': {} - {}", LIMITE.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE.getServiceName(), "status:error_business");
            throw new BusinessException(LIMITE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", LIMITE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE.getServiceName(), "status:success");
        return response;
    }
}