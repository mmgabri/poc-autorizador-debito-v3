package br.com.mmgabri.application.domains.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TransactionOperationEnum {
    DEBIT("DEBIT", false),
    CREDIT("CREDIT", true),
    REVERSAL("REVERSAL", true),
    PROVISIONING("PROVISIONING", false),
    ADVICE("ADVICE", false);

    private final String operationName;
    private final boolean accounting;

    TransactionOperationEnum(String operationName, boolean accounting) {
        this.accounting = accounting;
        this.operationName = operationName;
    }

    public static TransactionOperationEnum fromOperationName(String operationName) {
        return Arrays.stream(values())
                .filter(o -> o.operationName.equalsIgnoreCase(operationName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unexpected operation: " + operationName));
    }
}
