package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LedgerServiceGrpcStubProvider {

    private final RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub stub;

    @Value("${grpc.ledger-service-client.timeout}")
    private long timeoutMillis;

    public LedgerServiceGrpcStubProvider(RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
