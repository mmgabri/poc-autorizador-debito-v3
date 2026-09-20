package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LedgerGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.LedgerMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.ledger.v1.LedgerResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.LEDGER_SERVICE_SIMULATION;

// Simulação continua síncrona via gRPC. Efetivação é assíncrona (SQS/DynamoDB)
// e vive em LedgerEfetivacaoService — fluxos diferentes, clientes diferentes.
@Service
@RequiredArgsConstructor
public class LedgerServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceGrpcClient.class);
    private static final String TIPO_OPERACAO_SIMULACAO = "SIMULACAO";

    private final MetricsService metricsService;
    private final LedgerGrpcStubProvider ledgerGrpcStubProvider;
    private final LedgerMapper mapper;

    @SneakyThrows
    public LedgerResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToLedgerRequest(payload, TIPO_OPERACAO_SIMULACAO);
            logger.debug("Starting service call: {}", LEDGER_SERVICE_SIMULATION.getServiceName());

            var response = ledgerGrpcStubProvider.getStub().gerarLancamento(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", LEDGER_SERVICE_SIMULATION.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE_SIMULATION.getServiceName(), "status:error");
            throw new BusinessException(LEDGER_SERVICE_SIMULATION.getServiceName(), "999", e.getMessage());
        }
    }

    private LedgerResponse handleResponse(LedgerResponse response, OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", LEDGER_SERVICE_SIMULATION.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE_SIMULATION.getServiceName(), "status:error_business");
            throw new BusinessException(LEDGER_SERVICE_SIMULATION.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.debug("Service {} executed successfully", LEDGER_SERVICE_SIMULATION.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + LEDGER_SERVICE_SIMULATION.getServiceName(), "status:success");
        return response;
    }
}
