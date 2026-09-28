package br.com.mmgabri.application.exceptions;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;

public interface ServiceAwareException {
    String getService();
    ReasonCode getReasonCode();
    /** Catalog code (e.g. "AC-B001"); empty when there is no code. */
    String getErrorCode();
    String getErrorDescription();
}
