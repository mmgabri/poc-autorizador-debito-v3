package br.com.mmgabri.domain;

public record LedgerEfetivacaoResult(
        String correlationId,
        String contaId,
        boolean approved,
        String errorCode,
        String errorDescription
) {
}
