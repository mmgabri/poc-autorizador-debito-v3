package br.com.mmgabri.domain;

import lombok.Builder;

@Builder
public record LedgerEfetivacaoResponse(
        String correlationId,
        String contaId,
        boolean approved,
        String errorCode,
        String errorDescription
) {
}
