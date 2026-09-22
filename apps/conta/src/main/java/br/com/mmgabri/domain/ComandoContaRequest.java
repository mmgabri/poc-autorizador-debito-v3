package br.com.mmgabri.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ComandoContaRequest(
        String correlationId,
        String instanceId,
        String contaId,
        String customReturnLedger,
        long sleepLedgerEfetivacao
) {
}
