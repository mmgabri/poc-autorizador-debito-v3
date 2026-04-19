package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExecutionSimulationConfig {
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