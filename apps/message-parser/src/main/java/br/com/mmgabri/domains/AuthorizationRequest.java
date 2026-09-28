package br.com.mmgabri.domains;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Body of POST /authorization. The JSON names are the public REST contract
 * (used by k6/Postman) and are kept as-is via {@link JsonProperty}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AuthorizationRequest {
    private Map<String, String> messageIso;

    @JsonProperty("customReturnDataEnrichment")
    private String customReturnEnrichment;

    @JsonProperty("sleepDataEnrichment")
    private int sleepEnrichment;

    private String customReturnRules;
    private int sleepRules;

    @JsonProperty("customReturnSeguranca")
    private String customReturnSecurity;

    @JsonProperty("sleepSeguranca")
    private int sleepSecurity;

    private String customReturnLimit;

    @JsonProperty("sleepLimitEfetivacao")
    private int sleepLimitCommit;

    @JsonProperty("sleepLimitSimulacao")
    private int sleepLimitSimulation;

    @JsonProperty("customReturnLedger")
    private String customReturnAccountPosting;

    @JsonProperty("sleepLedgerEfetivacao")
    private int sleepAccountPostingCommit;

    @JsonProperty("sleepLedgerSimulacao")
    private int sleepAccountPostingSimulation;

    @JsonProperty("customReturnFraude")
    private String customReturnAntifraud;

    @JsonProperty("sleepFraude")
    private int sleepAntifraud;

    @JsonProperty("transactionIdReversal")
    private String reversalTransactionId;
}
