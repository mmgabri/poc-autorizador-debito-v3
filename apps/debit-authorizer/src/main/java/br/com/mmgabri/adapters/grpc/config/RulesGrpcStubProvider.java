package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RulesGrpcStubProvider {

    private final RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub stub;

    @Value("${grpc.rules-engine-client.timeout}")
    private long timeoutMillis;

    public RulesGrpcStubProvider(RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}
