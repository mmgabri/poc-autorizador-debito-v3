package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.antifraud.v1.AntifraudServiceGrpc;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AntiFraudGrpcStubProvider {

    private final AntifraudServiceGrpc.AntifraudServiceBlockingV2Stub stub;

    @Value("${grpc.antifraud-client.timeout}")
    private long timeoutMillis;

    public AntiFraudGrpcStubProvider(AntifraudServiceGrpc.AntifraudServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public AntifraudServiceGrpc.AntifraudServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}
