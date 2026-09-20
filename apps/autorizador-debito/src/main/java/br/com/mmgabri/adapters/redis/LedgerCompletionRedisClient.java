package br.com.mmgabri.adapters.redis;

import br.com.mmgabri.application.services.MetricsService;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class LedgerCompletionRedisClient {

    private static final Logger logger = LoggerFactory.getLogger(LedgerCompletionRedisClient.class);
    private static final String KEY_PREFIX = "efetivacao:ledger:";
    // Margem sobre o timeout do Redis - só pra não estourar timeout do lado
    // Java bem na hora que o BLPOP no servidor está prestes a retornar.
    private static final long JAVA_SIDE_SAFETY_MARGIN_MILLIS = 500;

    private final MetricsService metricsService;
    private final RedisConnectionFactory blockingRedisConnectionFactory;

    /**
     * BLPOP bloqueante aguardando o ledger-service sinalizar que terminou a
     * efetivação assíncrona (ver LedgerEfetivacaoService). O valor em si não
     * importa — é só um "acorda"; o resultado real fica no DynamoDB (fonte da
     * verdade), lido separadamente após o sinal.
     * <p>
     * Usa a API nativa do Lettuce em vez de StringRedisTemplate porque
     * ListOperations.leftPop(K, Duration) do Spring Data Redis arredonda
     * qualquer timeout sub-segundo pra cima, pro próximo segundo cheio
     * (TimeoutUtils.toSeconds) — um timeout de 600ms silenciosamente vira
     * 1000ms. O BLPOP nativo do Redis/Lettuce aceita timeout fracionário de
     * verdade (double, em segundos).
     * <p>
     * Por isso também usa uma {@code RedisConnectionFactory} dedicada
     * (blockingRedisConnectionFactory, sem conexão compartilhada) em vez da
     * conexão padrão da aplicação: BLPOP é bloqueante e monopoliza a conexão
     * até responder — numa conexão compartilhada isso travaria qualquer outro
     * comando Redis concorrente da aplicação.
     */
    public boolean awaitSignal(String correlationId, Duration timeout) {
        var startTime = OffsetDateTime.now();
        byte[] key = (KEY_PREFIX + correlationId).getBytes(StandardCharsets.UTF_8);
        double timeoutSeconds = timeout.toMillis() / 1000.0;

        try (RedisConnection connection = blockingRedisConnectionFactory.getConnection()) {
            if (!(connection instanceof LettuceConnection lettuceConnection)) {
                throw new IllegalStateException("blockingRedisConnectionFactory não é Lettuce - awaitSignal exige a API nativa para timeout sub-segundo.");
            }

            RedisClusterAsyncCommands<byte[], byte[]> commands = lettuceConnection.getNativeConnection();

            RedisFuture<KeyValue<byte[], byte[]>> future = commands.blpop(timeoutSeconds, key);
            KeyValue<byte[], byte[]> result = future.get(timeout.toMillis() + JAVA_SIDE_SAFETY_MARGIN_MILLIS, TimeUnit.MILLISECONDS);

            boolean signaled = result != null && result.hasValue();
            metricsService.incrementMetric("app_duration_redis_blpop", startTime, signaled ? "status:signaled" : "status:timeout");
            return signaled;
        } catch (Exception e) {
            logger.error("Erro ao aguardar sinal de conclusão do ledger no Redis. correlationId={}", correlationId, e);
            return false;
        }
    }
}
