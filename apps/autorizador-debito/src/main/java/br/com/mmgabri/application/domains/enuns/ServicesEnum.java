package br.com.mmgabri.application.domains.enuns;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum ServicesEnum {
    SEGURANCA("seguranca", false, true),
    DATA_ENRICHMENT("data-enrichment", false, true),
    LIMITE_PORTADOR("limite-portador", false, true),
    LIMITE("limite", true, false),
    LANCAMENTO_CONTA("lancamento-conta", true, false),
    FRAUDES("fraudes", false, true);

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


//    public static String getIsoCodeByErrorCode(String errorCode) {
//        for (ServicesEnum error : ServicesEnum.values()) {
//            if (error.errorCode.equals(errorCode)) {
//                return error.isoCode;
//            }
//        }
//        return "96";
//    }
}

