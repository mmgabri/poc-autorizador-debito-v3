package br.com.mmgabri.domain;

import lombok.Builder;

@Builder
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
