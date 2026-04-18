package br.com.mmgabri.adapter.grpc.config;

import br.com.mmgabri.grpc.RetornoContaServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AsyncBridgeGrpcStubProvider {

    private final RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub stub;

    @Value("${grpc.ledger-service-client.timeout}")
    private long timeoutMillis;

    public AsyncBridgeGrpcStubProvider(RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public RetornoContaServiceGrpc.RetornoContaServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
