package br.com.mmgabri.application.exceptions;

public interface ServiceAwareException {
    String getService();
    String getErrorCode();
    String getErrorDescription();
}
