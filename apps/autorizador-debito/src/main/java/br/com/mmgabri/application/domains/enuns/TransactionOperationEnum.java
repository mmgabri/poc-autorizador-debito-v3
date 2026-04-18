package br.com.mmgabri.application.domains.enuns;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum TransactionOperationEnum {
    DEBITO("DEBITO", false),
    CREDITO("CREDITO", true),
    ESTORNO("ESTORNO", true),
    PROVISIONAMENTO("PROVISIONAMENTO", false),
    ADVICE("ADVICE", false);

    private final String operationName;
    private final boolean isContabil;

    TransactionOperationEnum(String operationName, boolean isContabil) {
        this.isContabil = isContabil;
        this.operationName = operationName;
    }

    public static TransactionOperationEnum fromOperationName(String operationName) {
        return Arrays.stream(values())
                .filter(o -> o.operationName.equalsIgnoreCase(operationName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Operation não previsto: " + operationName));
    }
}

