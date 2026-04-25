package br.com.mmgabri.domains;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FormatadorRequest {
    private Map<String, String> messageIso;
    private String customReturnDataEnrichment;
    private int sleepDataEnrichment;
    private String customReturnRules;
    private int sleepRules;
    private String customReturnSeguranca;
    private int sleepSeguranca;
    private String customReturnLimit;
    private int sleepLimitEfetivacao;
    private int sleepLimitSimulacao;
    private String customReturnLedger;
    private int sleepLedgerEfetivacao;
    private int sleepLedgerSimulacao;
    private String customReturnFraude;
    private int sleepFraude;
    private String transactionIdReversal;
}

