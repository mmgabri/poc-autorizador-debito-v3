package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.domain.RedisCallbackMessage;
import br.com.mmgabri.services.PendingRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RetornoContaRedisSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(RetornoContaRedisSubscriber.class);

    private final PendingRequestService pendingRequestService;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            RedisCallbackMessage callback = objectMapper.readValue(payload, RedisCallbackMessage.class);
          //  logger.debug("Callback recebido do Redis. correlationId={}", callback.correlationId());
            pendingRequestService.complete(callback.correlationId(), callback);
        } catch (Exception e) {
            logger.error("Erro ao processar mensagem Redis", e);
        }
    }
}
