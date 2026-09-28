package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Simulation controls received in the request and forwarded to each dependency
 * (customReturn = outcome to simulate, sleep = artificial latency in ms).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ExecutionSimulationConfig {
    private String customReturnEnrichment;
    private int sleepEnrichment;
    private String customReturnRules;
    private int sleepRules;
    private String customReturnSecurity;
    private int sleepSecurity;
    private String customReturnLimit;
    private int sleepLimitCommit;
    private int sleepLimitSimulation;
    private String customReturnAccountPosting;
    private int sleepAccountPostingCommit;
    private int sleepAccountPostingSimulation;
    private String customReturnAntifraud;
    private int sleepAntifraud;
    private String reversalTransactionId;
}
