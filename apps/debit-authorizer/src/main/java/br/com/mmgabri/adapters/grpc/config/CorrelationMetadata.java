package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.application.domains.HeaderMessage;
import io.grpc.Metadata;
import io.grpc.stub.AbstractStub;
import io.grpc.stub.MetadataUtils;

/**
 * Attaches the transaction correlation context as gRPC metadata
 * (x-correlation-id / x-transaction-id) to every outgoing call.
 *
 * Explicit per call, and not through ThreadLocal/MDC in a global interceptor:
 * the dependencies are called in parallel on other threads (CompletableFuture on
 * the asyncTaskExecutor), so the context must come from the transaction Payload.
 */
public final class CorrelationMetadata {

    private static final Metadata.Key<String> CORRELATION_ID = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRANSACTION_ID = Metadata.Key.of("x-transaction-id", Metadata.ASCII_STRING_MARSHALLER);

    private CorrelationMetadata() {
    }

    public static <S extends AbstractStub<S>> S attach(S stub, HeaderMessage header) {
        var metadata = new Metadata();
        putIfPresent(metadata, CORRELATION_ID, header.getCorrelationId());
        putIfPresent(metadata, TRANSACTION_ID, header.getTransactionId());
        var interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        return stub.withInterceptors(interceptor);
    }

    private static void putIfPresent(Metadata metadata, Metadata.Key<String> key, String value) {
        if (value != null && !value.isBlank()) {
            metadata.put(key, value);
        }
    }
}
