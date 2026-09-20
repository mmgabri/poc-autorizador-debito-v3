package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.rules.v1.RulesServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RulesGrpcStubProvider {

    private final RulesServiceGrpc.RulesServiceBlockingV2Stub stub;

    @Value("${grpc.rules-service-client.timeout}")
    private long timeoutMillis;

    public RulesGrpcStubProvider(RulesServiceGrpc.RulesServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public RulesServiceGrpc.RulesServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}
