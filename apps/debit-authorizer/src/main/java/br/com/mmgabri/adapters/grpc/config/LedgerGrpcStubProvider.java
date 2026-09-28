package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import br.com.mmgabri.application.domains.HeaderMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class LedgerGrpcStubProvider {

    private final AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub stub;

    @Value("${grpc.account-posting-client.timeout}")
    private long timeoutMillis;

    public LedgerGrpcStubProvider(AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub stub) {
        this.stub = stub;
    }

    public AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub getStub(HeaderMessage header) {
        var stubWithDeadline = stub.withDeadlineAfter(timeoutMillis, TimeUnit.MILLISECONDS);
        return CorrelationMetadata.attach(stubWithDeadline, header);
    }
}
