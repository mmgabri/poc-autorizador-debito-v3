package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisPublisher.class);
    private static final String KEY_PREFIX = "efetivacao:ledger:";
    // TTL de segurança: limpa a chave sozinha caso o autorizador nunca faça o BLPOP
    // (timeout já estourado antes da efetivação terminar).
    private static final Duration KEY_TTL = Duration.ofSeconds(30);

    private final MetricsService metricsService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Sinal "acorda" para o autorizador aguardando via BLPOP — o valor em si não
     * importa, o resultado real da efetivação fica só no DynamoDB (fonte da verdade).
     */
    public void signal(String correlationId) {
        var startTime = OffsetDateTime.now();
        String key = KEY_PREFIX + correlationId;
        redisTemplate.opsForList().leftPush(key, OffsetDateTime.now().toString());
        redisTemplate.expire(key, KEY_TTL);
        metricsService.incrementMetric("app_ledger_duration_publish_redis", startTime);
        logger.debug("Sinal de conclusão publicado no Redis. key={}", key);
    }
}
