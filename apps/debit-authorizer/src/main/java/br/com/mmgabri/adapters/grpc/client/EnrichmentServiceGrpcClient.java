package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.DataEnrichmentGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.DataEnrichmentMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.ReasonCodeException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enums.ServicesEnum.ENRICHMENT_SERVICE;

@Service
@RequiredArgsConstructor
public class EnrichmentServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final DataEnrichmentGrpcStubProvider dataEnrichmentGrpcClient;
    private final DataEnrichmentMapper mapper;


    @SneakyThrows
    public EnrichTransactionResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToEnrichTransactionRequest(payload);
            logger.debug("Starting service call: {} ", ENRICHMENT_SERVICE.getServiceName());
            var stub = dataEnrichmentGrpcClient.getStub(payload.getHeaderMessage());
            var response = stub.enrichTransaction(request);
            return handleResponse(response, startTime);
        } catch (ReasonCodeException e) {
            throw e;
        } catch (Exception e) {
            var error = DependencyErrors.fromException(ENRICHMENT_SERVICE, e);
            logger.error("Service {} execution failed [{}]: {}", ENRICHMENT_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ENRICHMENT_SERVICE.getServiceName(), "status:error", "reason_code:" + error.getErrorCode());
            throw error;
        }
    }

    private EnrichTransactionResponse handleResponse(EnrichTransactionResponse response, OffsetDateTime startTime) {
        var result = response.getResult();
        if (!result.getApproved()) {
            var error = DependencyErrors.fromDeclined(ENRICHMENT_SERVICE, result);
            logger.error("Transaction denied by service '{}': {} - {}", ENRICHMENT_SERVICE.getServiceName(), error.getErrorCode(), error.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + ENRICHMENT_SERVICE.getServiceName(), "status:error_business", "reason_code:" + error.getErrorCode());
            throw error;
        }
        logger.debug("Service {} executed successfully", ENRICHMENT_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ ENRICHMENT_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
