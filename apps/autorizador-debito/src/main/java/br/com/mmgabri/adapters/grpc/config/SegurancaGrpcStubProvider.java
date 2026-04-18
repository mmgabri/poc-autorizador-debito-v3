package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.SegurancaServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class SegurancaGrpcStubProvider {

    private final SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub stub;

    @Value("${grpc.security-service-client.timeout}")
    private long timeoutMillis;

    public SegurancaGrpcStubProvider(SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}