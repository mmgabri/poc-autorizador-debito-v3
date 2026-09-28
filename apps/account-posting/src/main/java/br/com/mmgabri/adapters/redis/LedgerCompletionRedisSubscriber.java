package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.services.MetricsService;
import br.com.mmgabri.services.PendingCommitRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * SUB step: subscribes, from startup, to this instance's fixed channel
 * ({@code efetivacao:conta:{instanceId}} - see {@link br.com.mmgabri.config.AppConfig}).
 * When a notification arrives, releases the blocked gRPC call (if it still exists).
 */
@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisSubscriber.class);

    private final PendingCommitRegistry pendingRegistry;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        PostingResultPayload payload;
        try {
            payload = objectMapper.readValue(message.getBody(), PostingResultPayload.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize Redis channel message. channel={}", new String(message.getChannel()), e);
            return;
        }

        var postingResult = PostingResultPayloadMapper.toHandlePostingResultRequest(payload);
        var hadPendingFuture = pendingRegistry.complete(payload.correlationId(), postingResult);
        if (!hadPendingFuture) {
            logger.warn("Notification received without a pending future (timeout already fired?). correlationId={}", payload.correlationId());
            metricsService.incrementMetricCounter("app_ledger_future_nonexistent");
            // TODO: here the authorizer gRPC call already gave up (timeout) before the
            // conta response arrived - store the account command as COMPLETION_LATE instead
            // of falling into updateCompletedAck, which today treats both cases
            // (on time / late) as the same status.
        }

        logger.debug("Completion signal consumed via pub/sub. correlationId={}", payload.correlationId());
    }
}
