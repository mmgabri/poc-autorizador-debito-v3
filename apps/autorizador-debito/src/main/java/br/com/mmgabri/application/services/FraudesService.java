package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.rest.FraudesClient;
import br.com.mmgabri.application.domains.FraudesResponse;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.mappers.FraudesMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

import static br.com.mmgabri.application.domains.enuns.ServicesEnum.FRAUDES;

@Service
@RequiredArgsConstructor
public class FraudesService {
    private static final Logger logger = LoggerFactory.getLogger(FraudesService.class);

    private final FraudesClient client;
    private final FraudesMapper mapper;
    private final MetricsService metricsService;

    public FraudesResponse validarFraudes(Payload payload) {
        var startTime = OffsetDateTime.now();
        try {
            var response = client.validarFraude(mapper.mapToFraudesRequest(payload));
            return handleResponse(response, startTime);
        } catch (FeignException e) {
            logger.error("Service {} execution failed", FRAUDES.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + FRAUDES.getServiceName(), "status:error");
            throw new BusinessException(FRAUDES.getServiceName(), "999", e.status() + " / " + e.getMessage());

        } catch (Exception e) {
            logger.error("Service {} execution failed", FRAUDES.getServiceName());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + FRAUDES.getServiceName(), "status:error");
            throw new BusinessException(FRAUDES.getServiceName(), "999", e.getMessage());
        }
    }

    private FraudesResponse handleResponse(FraudesResponse response, OffsetDateTime startTime) {
        if (!response.isAprovado()) {
            logger.error("Transaction denied by service '{}': {} - {}", FRAUDES.getServiceName(), response.getErrorCode(), response.getErrorDescription());
            metricsService.incrementMetric("app_duration_service", startTime, "service:" + FRAUDES.getServiceName(), "status:error_business");
            throw new BusinessException(FRAUDES.getServiceName(), response.getErrorCode(), response.getErrorDescription());
        }
        logger.info("Service {} executed successfully", FRAUDES.getServiceName());
        metricsService.incrementMetric("app_duration_service", startTime, "service:" + FRAUDES.getServiceName(), "status:success");
        return response;
    }
}

