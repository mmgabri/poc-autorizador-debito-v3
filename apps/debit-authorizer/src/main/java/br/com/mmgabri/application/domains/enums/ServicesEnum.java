package br.com.mmgabri.application.domains.enums;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import lombok.Getter;

import java.util.Arrays;

import static br.com.itau.debit.authorizer.common.v1.ReasonCode.*;

@Getter
public enum ServicesEnum {
    ENRICHMENT_SERVICE("enrichment", false, true, REASON_CODE_AUTHORIZER_ENRICHMENT_TIMEOUT, REASON_CODE_AUTHORIZER_ENRICHMENT_UNAVAILABLE),
    RULES_SERVICE("rules-engine", false, true, REASON_CODE_AUTHORIZER_RULES_TIMEOUT, REASON_CODE_AUTHORIZER_RULES_UNAVAILABLE),
    SECURITY_SERVICE("security", false, true, REASON_CODE_AUTHORIZER_SECURITY_TIMEOUT, REASON_CODE_AUTHORIZER_SECURITY_UNAVAILABLE),
    LIMIT_SERVICE("limit", true, false, REASON_CODE_AUTHORIZER_LIMIT_TIMEOUT, REASON_CODE_AUTHORIZER_LIMIT_UNAVAILABLE),
    LEDGER_SERVICE("account-posting", true, false, REASON_CODE_AUTHORIZER_ACCOUNT_POSTING_TIMEOUT, REASON_CODE_AUTHORIZER_ACCOUNT_POSTING_UNAVAILABLE),
    LIMIT_SERVICE_SIMULATION("limit", false, true, REASON_CODE_AUTHORIZER_LIMIT_TIMEOUT, REASON_CODE_AUTHORIZER_LIMIT_UNAVAILABLE),
    LEDGER_SERVICE_SIMULATION("account-posting", false, true, REASON_CODE_AUTHORIZER_ACCOUNT_POSTING_TIMEOUT, REASON_CODE_AUTHORIZER_ACCOUNT_POSTING_UNAVAILABLE),
    ANTIFRAUD_SERVICE("antifraud", false, true, REASON_CODE_AUTHORIZER_ANTIFRAUD_TIMEOUT, REASON_CODE_AUTHORIZER_ANTIFRAUD_UNAVAILABLE);

    private final String serviceName;
    private final boolean hasCompensation;
    private final boolean registryAsync;
    // Codes assigned by the authorizer itself when the dependency does not answer
    // (the gRPC library generates the status, without ErrorInfo).
    private final ReasonCode timeoutReasonCode;
    private final ReasonCode unavailableReasonCode;

    ServicesEnum(String serviceName, boolean hasCompensation, boolean registryAsync, ReasonCode timeoutReasonCode, ReasonCode unavailableReasonCode) {
        this.serviceName = serviceName;
        this.hasCompensation = hasCompensation;
        this.registryAsync = registryAsync;
        this.timeoutReasonCode = timeoutReasonCode;
        this.unavailableReasonCode = unavailableReasonCode;
    }

    public static ServicesEnum fromServiceName(String serviceName) {
        return Arrays.stream(values())
                .filter(s -> s.serviceName.equalsIgnoreCase(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unexpected service: " + serviceName));
    }

}
