package br.com.mmgabri.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComandoContaRequest(
        String correlationId,
        String transactionId,
        String bandeira,
        String plataforma,
        String timestamp,
        String message,
        String contaId,
        String customReturnLedger,
        long sleepLedgerEfetivacao
) {
}
