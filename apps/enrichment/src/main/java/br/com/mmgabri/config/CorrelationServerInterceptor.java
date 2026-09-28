package br.com.mmgabri.config;

import io.grpc.ForwardingServerCallListener.SimpleForwardingServerCallListener;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.slf4j.MDC;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Reads the correlation context from gRPC metadata (x-correlation-id / x-transaction-id)
 * and exposes it in the MDC during each callback of the call, so every log line of
 * the processing is correlated. Correlation is observability context, so it travels
 * as metadata and not in the contract payload.
 *
 * The MDC is cleared at the end of each callback: the gRPC executor (virtual) thread
 * never carries context from one call to another.
 */
@Component
@GlobalServerInterceptor
public class CorrelationServerInterceptor implements ServerInterceptor {

    public static final String CORRELATION_ID_MDC = "correlationId";
    public static final String TRANSACTION_ID_MDC = "transactionId";

    private static final Metadata.Key<String> CORRELATION_ID = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRANSACTION_ID = Metadata.Key.of("x-transaction-id", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        var correlationId = headers.get(CORRELATION_ID);
        var transactionId = headers.get(TRANSACTION_ID);
        var context = new CorrelationContext(correlationId, transactionId);

        var listener = context.call(() -> next.startCall(call, headers));
        return new SimpleForwardingServerCallListener<>(listener) {
            @Override
            public void onMessage(ReqT message) {
                context.run(() -> super.onMessage(message));
            }

            @Override
            public void onHalfClose() {
                context.run(super::onHalfClose);
            }

            @Override
            public void onCancel() {
                context.run(super::onCancel);
            }

            @Override
            public void onComplete() {
                context.run(super::onComplete);
            }

            @Override
            public void onReady() {
                context.run(super::onReady);
            }
        };
    }

    private record CorrelationContext(String correlationId, String transactionId) {

        void run(Runnable action) {
            call(() -> {
                action.run();
                return null;
            });
        }

        <T> T call(Supplier<T> action) {
            putIfPresent(CORRELATION_ID_MDC, correlationId);
            putIfPresent(TRANSACTION_ID_MDC, transactionId);
            try {
                return action.get();
            } finally {
                MDC.remove(CORRELATION_ID_MDC);
                MDC.remove(TRANSACTION_ID_MDC);
            }
        }

        private static void putIfPresent(String key, String value) {
            if (value != null && !value.isBlank()) {
                MDC.put(key, value);
            }
        }
    }
}
