package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.LedgerServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LedgerGrpcStubProvider {

    private final LedgerServiceGrpc.LedgerServiceBlockingV2Stub stub;

    @Value("${grpc.ledger-service-client.timeout}")
    private long timeoutMillis;

    public LedgerGrpcStubProvider(LedgerServiceGrpc.LedgerServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public LedgerServiceGrpc.LedgerServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
