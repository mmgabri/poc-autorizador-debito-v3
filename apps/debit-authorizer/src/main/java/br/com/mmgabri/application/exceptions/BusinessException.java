package br.com.mmgabri.application.exceptions;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;

/**
 * Business decline: the dependency answered OK with a declined BusinessResult.
 */
public class BusinessException extends ReasonCodeException {

    public BusinessException(String service, ReasonCode reasonCode, String errorDescription) {
        super(service, reasonCode, errorDescription);
    }
}
