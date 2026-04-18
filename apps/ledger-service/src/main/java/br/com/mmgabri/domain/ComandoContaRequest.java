package br.com.mmgabri.domain;

public record ComandoContaRequest(
        String correlationId,
        String instanceId,
        String customReturnConta,
        long sleepConta
) {
}
