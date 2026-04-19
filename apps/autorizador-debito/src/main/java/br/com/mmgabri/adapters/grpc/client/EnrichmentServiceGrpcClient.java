package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.DataEnrichmentGrpcStubProvider;
import br.com.mmgabri.adapters.grpc.mappers.DataEnrichmentMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.grpc.EnrichByCardResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.ENRICHMENT_SERVICE;

@Service
@RequiredArgsConstructor
public class EnrichmentServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(EnrichmentServiceGrpcClient.class);

    private final MetricsService metricsService;
    private final DataEnrichmentGrpcStubProvider dataEnrichmentGrpcClient;
    private final DataEnrichmentMapper mapper;


    @SneakyThrows
    public EnrichByCardResponse execute(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var request = mapper.payloadToEnrichByCardRequestRequest(payload);
            logger.debug("Starting service call: {} ", ENRICHMENT_SERVICE.getServiceName());
            var response = dataEnrichmentGrpcClient.getStub().enrichByCard(request);
            return handleResponse(response, startTime);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Service {} execution failed", ENRICHMENT_SERVICE.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ ENRICHMENT_SERVICE.getServiceName(), "status:error");
            throw new BusinessException(ENRICHMENT_SERVICE.getServiceName(), "999", e.getMessage());
        }
    }

    private EnrichByCardResponse handleResponse(EnrichByCardResponse response, OffsetDateTime startTime) {
        if (!response.getApproved()) {
            logger.error("Transaction denied by service '{}': {} - {}", ENRICHMENT_SERVICE.getServiceName(),  response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:"+ ENRICHMENT_SERVICE.getServiceName(), "status:error_business");
            throw new BusinessException(ENRICHMENT_SERVICE.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", ENRICHMENT_SERVICE.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:"+ ENRICHMENT_SERVICE.getServiceName(), "status:success");
        return response;
    }
}
