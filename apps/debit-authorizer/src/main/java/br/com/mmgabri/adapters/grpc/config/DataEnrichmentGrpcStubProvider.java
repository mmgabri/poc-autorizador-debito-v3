package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.enrichment.v1.EnrichmentServiceGrpc;
import lombok.AllArgsConstructor;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;


@Component
public class DataEnrichmentGrpcStubProvider {

    private final EnrichmentServiceGrpc.EnrichmentServiceBlockingV2Stub stub;

    @Value("${grpc.enrichment-client.timeout}")
    private long timeoutMillis;

    public DataEnrichmentGrpcStubProvider(EnrichmentServiceGrpc.EnrichmentServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public EnrichmentServiceGrpc.EnrichmentServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}