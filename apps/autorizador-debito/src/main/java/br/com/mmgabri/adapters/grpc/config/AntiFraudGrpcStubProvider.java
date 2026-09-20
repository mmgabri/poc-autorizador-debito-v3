package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.antifraud.v1.AntiFraudServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AntiFraudGrpcStubProvider {

    private final AntiFraudServiceGrpc.AntiFraudServiceBlockingV2Stub stub;

    @Value("${grpc.antifraud-service-client.timeout}")
    private long timeoutMillis;

    public AntiFraudGrpcStubProvider(AntiFraudServiceGrpc.AntiFraudServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public AntiFraudServiceGrpc.AntiFraudServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
