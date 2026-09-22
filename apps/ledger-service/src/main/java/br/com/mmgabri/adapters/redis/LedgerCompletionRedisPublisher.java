package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import br.com.mmgabri.services.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Passo PUB: publica o resultado real no canal fixo da instância que
 * despachou (roteado por instanceId — ver {@link RetornoContaPayload}).
 */
@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisPublisher.class);
    public static final String CHANNEL_PREFIX = "efetivacao:conta:";

    private final MetricsService metricsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(RetornoContaRequest retorno) {
        var startTime = OffsetDateTime.now();
        String channel = CHANNEL_PREFIX + retorno.getInstanceId();
        var payload = new RetornoContaPayload(
                retorno.getCorrelationId(),
                retorno.getApproved(),
                retorno.getErrorCode(),
                retorno.getErrorDescription()
        );
        redisTemplate.convertAndSend(channel, toJson(payload));
        metricsService.incrementMetric("app_ledger_duration_publish_redis", startTime);
        logger.debug("Resultado publicado no canal Redis. channel={} correlationId={}", channel, retorno.getCorrelationId());
    }

    private String toJson(RetornoContaPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao serializar RetornoContaPayload", e);
        }
    }
}
