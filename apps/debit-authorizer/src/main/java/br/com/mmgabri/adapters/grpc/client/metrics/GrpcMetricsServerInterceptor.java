package br.com.mmgabri.adapters.grpc.client.metrics;

import io.grpc.*;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class GrpcMetricsServerInterceptor implements ServerInterceptor {

    private final MeterRegistry registry;
    private final ConcurrentHashMap<Key, Timer> timers = new ConcurrentHashMap<>();

    public GrpcMetricsServerInterceptor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        final String method = call.getMethodDescriptor().getFullMethodName();
        final long startNanos = System.nanoTime();

        ServerCall<ReqT, RespT> monitoringCall =
                new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
                    @Override
                    public void close(Status status, Metadata trailers) {
                        long tookNanos = System.nanoTime() - startNanos;
                        String statusCode = status.getCode().name();

                        Timer t = timers.computeIfAbsent(
                                new Key(method, statusCode),
                                k -> Timer.builder("grpc_autorizador_server_requests")
                                        .tag("method", k.method)
                                        .tag("status", k.status)
                                        .register(registry)
                        );

                        t.record(tookNanos, TimeUnit.NANOSECONDS);
                        super.close(status, trailers);
                    }
                };

        return next.startCall(monitoringCall, headers);
    }

    private record Key(String method, String status) {
    }
}
