package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LimitePortadorGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LimitePortadorMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.grpc.LimitePortadorResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.*;

@Service
@RequiredArgsConstructor
public class LimitePortadorGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LimitePortadorGrpcClient.class);

    private final MetricsService metricsService;
    private final LimitePortadorGrpcStubProvider limitePortadorGrpcClient;
    private final LimitePortadorMapper mapper;
    private final TransactionContextRegistryService transactionRegistry;

    @SneakyThrows
    public LimitePortadorResponse execute(Payload payload, boolean isReversal) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLimitePortadorRequest(payload, isReversal);
            logger.debug("Starting service call: {} - isReversal: {}" , LIMITE_PORTADOR.getServiceName(), isReversal);
            var response = limitePortadorGrpcClient.getStub().atualizarLimitePortador(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LIMITE_PORTADOR.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE_PORTADOR.getServiceName(), "status:error");
            throw new BusinessException(LIMITE_PORTADOR.getServiceName(), "999", e.getMessage());
        }
    }

    private LimitePortadorResponse handleResponse(LimitePortadorResponse response, OffsetDateTime startTime) {
        if (!response.getAprovado()) {
            logger.error("Transaction denied by service '{}': {} - {}", LIMITE_PORTADOR.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE_PORTADOR.getServiceName(), "status:error_business");
            throw new BusinessException(LIMITE_PORTADOR.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", LIMITE_PORTADOR.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+LIMITE_PORTADOR.getServiceName(), "status:success");
        return response;
    }
}