package br.com.mmgabri.domain;

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
