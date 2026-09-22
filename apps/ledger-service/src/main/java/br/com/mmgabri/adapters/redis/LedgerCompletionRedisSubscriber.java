package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import br.com.mmgabri.services.MetricsService;
import br.com.mmgabri.services.PendingEfetivacaoRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

/**
 * Passo SUB: assina, desde o startup, o canal fixo desta instância
 * ({@code efetivacao:conta:{instanceId}} — ver
 * {@link br.com.mmgabri.config.AppConfig}). Ao receber uma notificação,
 * destrava o gRPC bloqueado (se ainda existir) e marca COMPLETED_ACK.
 */
@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisSubscriber implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisSubscriber.class);

    private final PendingEfetivacaoRegistry pendingRegistry;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        RetornoContaPayload payload;
        try {
            payload = objectMapper.readValue(message.getBody(), RetornoContaPayload.class);
        } catch (Exception e) {
            logger.error("Falha ao desserializar mensagem do canal Redis. channel={}", new String(message.getChannel()), e);
            return;
        }

        var retornoContaRequest = toRetornoContaRequest(payload);
        var hadPendingFuture = pendingRegistry.complete(payload.correlationId(), retornoContaRequest);
        if (!hadPendingFuture) {
            logger.warn("Notificação recebida sem future pendente (timeout já disparou?). correlationId={}", payload.correlationId());
            metricsService.incrementMetricCounter("app_efetivacao_completion_late");
            // TODO: nesse caso o gRPC do autorizador já desistiu (timeout) antes da
            // resposta do conta chegar - gravar comando_conta como COMPLETION_LATE em
            // vez de cair no updateCompletedAck logo abaixo, que hoje trata os dois
            // casos (a tempo / tarde) como se fossem o mesmo status.
        }

        logger.debug("Sinal de conclusão consumido via pub/sub. correlationId={}", payload.correlationId());
    }

    private RetornoContaRequest toRetornoContaRequest(RetornoContaPayload payload) {
        return RetornoContaRequest.newBuilder()
                .setCorrelationId(payload.correlationId())
                .setApproved(payload.approved())
                .setErrorCode(payload.errorCode() == null ? "" : payload.errorCode())
                .setErrorDescription(payload.errorDescription() == null ? "" : payload.errorDescription())
                .build();
    }
}
