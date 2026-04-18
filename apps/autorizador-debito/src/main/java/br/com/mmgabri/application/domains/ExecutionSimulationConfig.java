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
    private String customReturnSeguranca;
    private String customReturnLimitePortador;
    private String customReturnLimite;
    private String customReturnLancamentoConta;
    private String customReturnFraude;
    private String transactionIdReversal;
    private int sleepSeguranca;
    private int sleepDataEnrichment;
    private int sleepLimitePortador;
    private int sleepLimite;
    private int sleepLancamentoConta;
    private int sleepFraude;
}