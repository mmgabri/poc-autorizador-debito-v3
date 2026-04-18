package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.LimitePortadorServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class LimitePortadorGrpcStubProvider {

    private final LimitePortadorServiceGrpc.LimitePortadorServiceBlockingV2Stub stub;

    @Value("${grpc.rules-service-client.timeout}")
    private long timeoutMillis;

    public LimitePortadorGrpcStubProvider(LimitePortadorServiceGrpc.LimitePortadorServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public LimitePortadorServiceGrpc.LimitePortadorServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}