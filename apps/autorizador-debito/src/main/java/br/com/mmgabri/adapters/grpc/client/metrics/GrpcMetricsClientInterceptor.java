package br.com.mmgabri.adapters.grpc.client.metrics;

import io.grpc.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcMetricsClientInterceptor implements ClientInterceptor {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<Key, Timer> timers = new ConcurrentHashMap<>();

    public GrpcMetricsClientInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next
    ) {
        final String fullMethod = method.getFullMethodName(); // pkg.Service/Method

        ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);

        return new ForwardingClientCall.SimpleForwardingClientCall<>(call) {

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {

                final long startNanos = System.nanoTime(); // <-- por chamada (start)

                Listener<RespT> monitoringListener =
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {

                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                long tookNanos = System.nanoTime() - startNanos;
                                String statusCode = status.getCode().name();

                                Timer t = timers.computeIfAbsent(
                                        new Key(fullMethod, statusCode),
                                        k -> Timer.builder("grpc_autorizador_client_requests")
                                                .tag("method", k.method)
                                                .tag("status", k.status)
                                                .register(meterRegistry)
                                );
                                t.record(tookNanos, TimeUnit.NANOSECONDS);

                                super.onClose(status, trailers);
                            }
                        };

                super.start(monitoringListener, headers);
            }
        };
    }

    private record Key(String method, String status) {
    }
}
