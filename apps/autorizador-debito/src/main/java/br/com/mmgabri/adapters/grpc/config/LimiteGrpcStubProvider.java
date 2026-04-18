package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.LimiteServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class LimiteGrpcStubProvider {

    private final LimiteServiceGrpc.LimiteServiceBlockingV2Stub stub;

    @Value("${grpc.limit-service-client.timeout}")
    private long timeoutMillis;

    public LimiteGrpcStubProvider(LimiteServiceGrpc.LimiteServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public LimiteServiceGrpc.LimiteServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}