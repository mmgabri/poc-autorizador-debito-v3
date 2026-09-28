package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges the synchronous gRPC call (blocked on {@code future.get}) with the
 * asynchronous confirmation that arrives via Redis pub/sub - an entry only exists
 * while the authorizer gRPC call is waiting; it leaves the map as soon as it
 * resolves (by signal or by timeout).
 */
@Component
public class PendingCommitRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<HandlePostingResultRequest>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<HandlePostingResultRequest> register(String correlationId) {
        CompletableFuture<HandlePostingResultRequest> future = new CompletableFuture<>();
        pending.put(correlationId, future);
        return future;
    }

    public void remove(String correlationId) {
        pending.remove(correlationId);
    }

    /**
     * Called by the Redis subscriber (SUB step). Returns {@code false} without error
     * when there is no pending future anymore - the timeout may have fired before the
     * notification arrived; that is expected, not a failure.
     */
    public boolean complete(String correlationId, HandlePostingResultRequest result) {
        CompletableFuture<HandlePostingResultRequest> future = pending.remove(correlationId);
        if (future == null) {
            return false;
        }
        return future.complete(result);
    }
}
