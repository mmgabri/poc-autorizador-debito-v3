package br.com.mmgabri.services;

import br.com.mmgabri.domain.RedisCallbackMessage;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@RequiredArgsConstructor
public class PendingRequestService {

    private static final Logger logger = LoggerFactory.getLogger(PendingRequestService.class);

    private final MetricsService metricsService;

    @Value("${app.async.response-timeout-seconds:30}")
    private long responseTimeoutSeconds;

    private final ConcurrentHashMap<String, CompletableFuture<RedisCallbackMessage>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<RedisCallbackMessage> register(String correlationId) {
        CompletableFuture<RedisCallbackMessage> future = new CompletableFuture<>();
        pending.put(correlationId, future);
        logger.debug("Future registrado. correlationId={}", correlationId);
        return future;
    }

    @SneakyThrows
    public RedisCallbackMessage waitForCallback(String correlationId, CompletableFuture<RedisCallbackMessage> future) {
        try {
            logger.debug("Aguardando callback. correlationId={}", correlationId);
            var result = future.get(responseTimeoutSeconds, TimeUnit.SECONDS);
            logger.debug("Callback recebido. correlationId={}", correlationId);
            return result;
        } catch (TimeoutException e) {
            metricsService.incrementMetricCounter("app_ledger_timeout_callback");
            logger.error("Timeout aguardando callback. correlationId={}", correlationId);
            throw e;
        } finally {
            pending.remove(correlationId);
        }
    }

    public void complete(String correlationId, RedisCallbackMessage message) {
        CompletableFuture<RedisCallbackMessage> future = pending.get(correlationId);
        if (future != null) {
            future.complete(message);
            logger.debug("Future completado. correlationId={}", correlationId);
        } else {
            metricsService.incrementMetricCounter("app_ledger_future_nonexistent");
            logger.error("Nenhum future pendente para correlationId={}", correlationId);
        }
    }
}
