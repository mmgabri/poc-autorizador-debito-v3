package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.security.v1.SecurityServiceGrpc;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class SecurityGrpcStubProvider {

    private final SecurityServiceGrpc.SecurityServiceBlockingV2Stub stub;

    @Value("${grpc.security-client.timeout}")
    private long timeoutMillis;

    public SecurityGrpcStubProvider(SecurityServiceGrpc.SecurityServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public SecurityServiceGrpc.SecurityServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}