package br.com.mmgabri.services;

import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pontes a chamada gRPC síncrona (bloqueada em {@code future.get}) com a
 * confirmação assíncrona que chega via Redis pub/sub — só existe enquanto o
 * gRPC do autorizador está esperando; some do mapa assim que resolve (por
 * sinal ou por timeout).
 */
@Component
public class PendingEfetivacaoRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<RetornoContaRequest>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<RetornoContaRequest> register(String correlationId) {
        CompletableFuture<RetornoContaRequest> future = new CompletableFuture<>();
        pending.put(correlationId, future);
        return future;
    }

    public void remove(String correlationId) {
        pending.remove(correlationId);
    }

    /**
     * Chamado pelo subscriber do Redis (passo SUB). Retorna {@code false} sem
     * erro quando não há mais future pendente — pode ter estourado timeout
     * antes da notificação chegar; é esperado, não uma falha.
     */
    public boolean complete(String correlationId, RetornoContaRequest result) {
        CompletableFuture<RetornoContaRequest> future = pending.remove(correlationId);
        if (future == null) {
            return false;
        }
        return future.complete(result);
    }
}
