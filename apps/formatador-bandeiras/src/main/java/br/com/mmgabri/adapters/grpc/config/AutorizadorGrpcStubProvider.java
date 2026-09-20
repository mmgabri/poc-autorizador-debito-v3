package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.autorizador.v1.AutorizadorServiceGrpc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class AutorizadorGrpcStubProvider {

    private final AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub stub;

    @Value("${grpc.autorizador-client.timeout}")
    private long timeoutMillis;

    public AutorizadorGrpcStubProvider(AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}