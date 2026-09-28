package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizationServiceGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AuthorizerGrpcStubProvider {

    private static final Metadata.Key<String> CORRELATION_ID = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    private final AuthorizationServiceGrpc.AuthorizationServiceBlockingV2Stub stub;

    @Value("${grpc.debit-authorizer-client.timeout}")
    private long timeoutMillis;

    public AuthorizerGrpcStubProvider(AuthorizationServiceGrpc.AuthorizationServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    /**
     * Stub with deadline and the correlationId as gRPC metadata (x-correlation-id),
     * used by the authorizer to correlate logs. The same value also travels in the
     * payload, where it is the idempotency key.
     */
    public AuthorizationServiceGrpc.AuthorizationServiceBlockingV2Stub getStub(String correlationId) {
        var metadata = new Metadata();
        metadata.put(CORRELATION_ID, correlationId);
        var interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return stubWithDeadline.withInterceptors(interceptor);
    }
}
