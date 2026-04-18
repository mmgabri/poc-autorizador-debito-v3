package br.com.mmgabri.application.exceptions;

public class TechnicalException extends RuntimeException {

    private String stepName;
    private String errorCode;
    private String errorDescription;

    public TechnicalException(String stepName, String errorCode, String errorDescription) {
        super(String.format("Service: %s | Error Code: %s | Description: %s", stepName, errorCode, errorDescription));
        this.stepName = stepName;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}

