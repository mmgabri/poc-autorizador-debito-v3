package br.com.mmgabri.application.exceptions;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.mmgabri.application.utils.ReasonCodes;

/**
 * Base of the failures that carry a catalog code (business or technical).
 */
public abstract class ReasonCodeException extends RuntimeException implements ServiceAwareException {

    private final String service;
    private final ReasonCode reasonCode;
    private final String errorCode;
    private final String errorDescription;

    protected ReasonCodeException(String service, ReasonCode reasonCode, String errorDescription) {
        super(String.format("Service: %s | Error Code: %s | Description: %s", service, codeOf(reasonCode), errorDescription));
        this.service = service;
        this.reasonCode = reasonCode;
        this.errorCode = codeOf(reasonCode);
        this.errorDescription = errorDescription;
    }

    @Override
    public String getService() {
        return service;
    }

    @Override
    public ReasonCode getReasonCode() {
        return reasonCode;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorDescription() {
        return errorDescription;
    }

    private static String codeOf(ReasonCode reasonCode) {
        if (reasonCode == ReasonCode.REASON_CODE_UNSPECIFIED || reasonCode == ReasonCode.UNRECOGNIZED) {
            return "";
        }
        return ReasonCodes.codeOf(reasonCode);
    }
}
