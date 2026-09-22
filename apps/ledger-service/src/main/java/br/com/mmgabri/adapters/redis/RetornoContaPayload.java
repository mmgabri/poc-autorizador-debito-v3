package br.com.mmgabri.adapters.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpo da mensagem publicada/consumida no canal pub/sub
 * {@code efetivacao:conta:{instanceId}} — o correlationId identifica qual
 * future local completar, já que o canal é fixo por instância (não por
 * transação).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RetornoContaPayload(
        String correlationId,
        boolean approved,
        String errorCode,
        String errorDescription
) {
}
