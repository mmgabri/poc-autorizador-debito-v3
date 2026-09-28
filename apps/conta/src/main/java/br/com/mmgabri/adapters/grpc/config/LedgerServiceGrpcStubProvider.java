package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LedgerServiceGrpcStubProvider {

    private static final Metadata.Key<String> CORRELATION_ID = Metadata.Key.of("x-correlation-id", Metadata.ASCII_STRING_MARSHALLER);

    private final AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub stub;

    @Value("${grpc.account-posting-client.timeout}")
    private long timeoutMillis;

    public LedgerServiceGrpcStubProvider(AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    /**
     * Stub with deadline and the correlationId as gRPC metadata (x-correlation-id),
     * used by account-posting to correlate the callback logs.
     */
    public AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub getStub(String correlationId) {
        var metadata = new Metadata();
        metadata.put(CORRELATION_ID, correlationId);
        var interceptor = MetadataUtils.newAttachHeadersInterceptor(metadata);
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return stubWithDeadline.withInterceptors(interceptor);
    }
}
