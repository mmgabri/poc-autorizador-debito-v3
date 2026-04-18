package br.com.mmgabri.domain;

public record RedisCallbackMessage(
        String correlationId,
        boolean aprovado,
        String errorCode,
        String errorDescription
) {
}
