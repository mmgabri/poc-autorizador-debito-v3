package br.com.mmgabri.application.domains.enuns;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ServicesEnum {
    ENRICHMENT_SERVICE("enrichment-service", false, true),
    RULES_SERVICE("rules-service", false, true),
    SECURITY_SERVICE("security-service", false, true),
    LIMIT_SERVICE("limit-service", true, false),
    LEDGER_SERVICE("ledger-service", true, false),
    LIMIT_SERVICE_SIMULATION("limit-service", false, true),
    LEDGER_SERVICE_SIMULATION("ledger-service", false, true),
    ANTIFRAUD_SERVICE("antifraud-service", false, true);

    private final String serviceName;
    private final boolean hasCompensation;
    private final boolean registryAsync;

    ServicesEnum(String serviceName, boolean hasCompensation, boolean registryAsync) {
        this.serviceName = serviceName;
        this.hasCompensation = hasCompensation;
        this.registryAsync = registryAsync;
    }

    public static ServicesEnum fromServiceName(String serviceName) {
        return Arrays.stream(values())
                .filter(s -> s.serviceName.equalsIgnoreCase(serviceName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service não previsto: " + serviceName));
    }

}

