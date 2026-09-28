package br.com.mmgabri.adapters.redis;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.mmgabri.services.MetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * PUB step: publishes the actual result on the fixed channel of the dispatching
 * instance (routed by instanceId - see {@link PostingResultPayload}).
 */
@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisPublisher.class);
    public static final String CHANNEL_PREFIX = "efetivacao:conta:";

    private final MetricsService metricsService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(HandlePostingResultRequest postingResult) {
        var startTime = OffsetDateTime.now();
        String channel = CHANNEL_PREFIX + postingResult.getInstanceId();
        var payload = PostingResultPayloadMapper.toPayload(postingResult);
        redisTemplate.convertAndSend(channel, toJson(payload));
        metricsService.incrementMetric("app_ledger_duration_publish_redis", startTime);
        logger.debug("Result published on Redis channel. channel={} correlationId={}", channel, postingResult.getCorrelationId());
    }

    private String toJson(PostingResultPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize PostingResultPayload", e);
        }
    }
}
