package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.domain.RedisCallbackMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetornoContaRedisPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RetornoContaRedisPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publish(String instanceId, RedisCallbackMessage message) {
        String channel = "channel:" + instanceId + ":" + message.correlationId();
        try {
            String payload = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(channel, payload);
            logger.debug("Publicado no Redis. channel={}", channel);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erro ao serializar RedisCallbackMessage", e);
        }
    }
}
