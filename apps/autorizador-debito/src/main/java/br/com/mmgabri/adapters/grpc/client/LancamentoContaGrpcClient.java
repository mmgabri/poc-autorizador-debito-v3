package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LancamentoContaGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LancamentoContaMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.grpc.LancamentoContaResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LANCAMENTO_CONTA;

@Service
@RequiredArgsConstructor
public class LancamentoContaGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LancamentoContaGrpcClient.class);

    private final MetricsService metricsService;
    private final LancamentoContaGrpcStubProvider lancamentoContaGrpcClient;
    private final LancamentoContaMapper mapper;
    private final TransactionContextRegistryService transactionRegistry;

    @SneakyThrows
    public LancamentoContaResponse execute(Payload payload, boolean isReversal) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLancamentoContaRequest(payload, isReversal);
            logger.debug("Starting service call: {} - isReversal: {}" , LANCAMENTO_CONTA.getServiceName(), isReversal);
            var response = lancamentoContaGrpcClient.getStub().gerarLancamento(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LANCAMENTO_CONTA.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LANCAMENTO_CONTA.getServiceName(), "status:error");
            throw new BusinessException(LANCAMENTO_CONTA.getServiceName(), "999", e.getMessage());
        }
    }

    private LancamentoContaResponse handleResponse(LancamentoContaResponse response, OffsetDateTime startTime) {
        if (!response.getAprovado()) {
            logger.error("Transaction denied by service '{}': {} - {}", LANCAMENTO_CONTA.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+LANCAMENTO_CONTA.getServiceName(), "status:error_business");
            throw new BusinessException(LANCAMENTO_CONTA.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", LANCAMENTO_CONTA.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+LANCAMENTO_CONTA.getServiceName(), "status:success");
        return response;
    }
}