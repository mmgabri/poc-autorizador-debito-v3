package br.com.mmgabri.application.exceptions;

public class BusinessException extends RuntimeException implements ServiceAwareException {

    private String service;
    private String errorCode;
    private String errorDescription;

    public BusinessException(String stepName, String errorCode, String errorDescription) {
        super(String.format("Service: %s | Error Code: %s | Description: %s", stepName, errorCode, errorDescription));
        this.service = stepName;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
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

