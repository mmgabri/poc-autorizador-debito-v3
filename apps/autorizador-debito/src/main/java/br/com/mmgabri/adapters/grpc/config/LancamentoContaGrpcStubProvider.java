package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.LancamentoContaServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class LancamentoContaGrpcStubProvider {

    private final LancamentoContaServiceGrpc.LancamentoContaServiceBlockingV2Stub stub;

    @Value("${grpc.ledger-service-client.timeout}")
    private long timeoutMillis;

    public LancamentoContaGrpcStubProvider(LancamentoContaServiceGrpc.LancamentoContaServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public LancamentoContaServiceGrpc.LancamentoContaServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}