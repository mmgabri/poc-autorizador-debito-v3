package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.limit.v1.LimitServiceGrpc;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class LimitGrpcStubProvider {

    private final LimitServiceGrpc.LimitServiceBlockingV2Stub stub;

    @Value("${grpc.limit-client.timeout}")
    private long timeoutMillis;

    public LimitGrpcStubProvider(LimitServiceGrpc.LimitServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public LimitServiceGrpc.LimitServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}