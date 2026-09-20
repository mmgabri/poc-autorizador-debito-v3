package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.enrichment.v1.DataEnrichmentServiceGrpc;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class DataEnrichmentGrpcStubProvider {

    private final DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub stub;

    @Value("${grpc.enrichment-service-client.timeout}")
    private long timeoutMillis;

    public DataEnrichmentGrpcStubProvider(DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub getStub() {
        return stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
    }
}