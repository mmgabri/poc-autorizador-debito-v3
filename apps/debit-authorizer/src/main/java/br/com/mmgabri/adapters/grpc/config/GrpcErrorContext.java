package br.com.mmgabri.adapters.grpc.config;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;

public class GrpcErrorContext {
    private final Throwable throwable;
    private final AuthorizeTransactionRequest request;

    public GrpcErrorContext(Throwable throwable, AuthorizeTransactionRequest request) {
        this.throwable = throwable;
        this.request = request;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public AuthorizeTransactionRequest getRequest() {
        return request;
    }
}
