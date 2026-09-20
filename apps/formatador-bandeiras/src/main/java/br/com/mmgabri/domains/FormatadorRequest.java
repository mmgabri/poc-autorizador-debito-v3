package br.com.mmgabri.domains;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
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

