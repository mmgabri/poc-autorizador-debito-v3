package br.com.mmgabri.adapters.grpc.config;

import br.com.mmgabri.grpc.AutorizadorRequest;

public class GrpcErrorContext {
    private final Throwable throwable;
    private final AutorizadorRequest request;

    public GrpcErrorContext(Throwable throwable, AutorizadorRequest request) {
        this.throwable = throwable;
        this.request = request;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public AutorizadorRequest getRequest() {
        return request;
    }
}
