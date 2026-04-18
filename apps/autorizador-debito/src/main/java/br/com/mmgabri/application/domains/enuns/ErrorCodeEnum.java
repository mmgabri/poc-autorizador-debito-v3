package br.com.mmgabri.application.domains.enuns;

public enum ErrorCodeEnum {
    SDO("SDO", "51"),
    LIM("LIM", "63"),
    SEN("SEN", "55"),
    CHP("CHP", "88"),
    CVV("CVV", "57"),
    CNE("CNE", "14"),
    IND("IND", "96"),
    TIM("TIM", "91"),
    EIN("EIN", "96"),
    ERR("ERR", "96");

    private final String errorCode;
    private final String isoCode;

    ErrorCodeEnum(String errorCode, String isoCode) {
        this.errorCode = errorCode;
        this.isoCode = isoCode;
    }

    public static String getIsoCodeByErrorCode(String errorCode) {
        for (ErrorCodeEnum error : ErrorCodeEnum.values()) {
            if (error.errorCode.equals(errorCode)) {
                return error.isoCode;
            }
        }
        return "96";
    }
}

