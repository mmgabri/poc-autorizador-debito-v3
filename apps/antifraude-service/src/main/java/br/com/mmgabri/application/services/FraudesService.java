package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.FraudesRequest;
import br.com.mmgabri.application.domains.FraudesResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class FraudesService {
    private static final Logger logger = LoggerFactory.getLogger(FraudesService.class);

    private final MotorFraudesService motorFraudesService;
    private final ExecutorService vtExecutor;
    private final MetricsService metricsService;

    public CompletionStage<FraudesResponse> validarFraude(FraudesRequest request) {
        var startTime = OffsetDateTime.now();
        return CompletableFuture.supplyAsync(() -> {
            var response = motorFraudesService.execute(request);
            logger.info("Fraudes completed in {} ms.", Duration.between(startTime, OffsetDateTime.now()).toMillis());
            metricsService.incrementMetric("app_frau_duration_transaction", startTime);
            return response;
        }, vtExecutor);
    }
}
