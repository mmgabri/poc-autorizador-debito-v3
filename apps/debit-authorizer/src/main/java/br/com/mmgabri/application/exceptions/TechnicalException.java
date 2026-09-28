package br.com.mmgabri.application.exceptions;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;

/**
 * Technical failure: the dependency could not answer (gRPC error with ErrorInfo,
 * timeout, unavailability) or the failure is in the authorizer itself.
 */
public class TechnicalException extends ReasonCodeException {

    public TechnicalException(String service, ReasonCode reasonCode, String errorDescription) {
        super(service, reasonCode, errorDescription);
    }
}
