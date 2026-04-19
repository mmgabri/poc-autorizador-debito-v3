package br.com.mmgabri.domain;

public record RedisCallbackMessage(
        String correlationId,
        boolean approved,
        String errorCode,
        String errorDescription
) {
}
