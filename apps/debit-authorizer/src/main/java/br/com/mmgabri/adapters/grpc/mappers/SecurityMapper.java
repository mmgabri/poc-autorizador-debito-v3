package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityRequest;
import org.springframework.stereotype.Component;

@Component
public class SecurityMapper {

    public ValidateSecurityRequest payloadToValidateSecurityRequest(Payload payload) {

        return ValidateSecurityRequest.newBuilder()
                .setAccountId(payload.getDataEnrichment().getAccount().getAccountId())
                .setChipData(payload.getMessageIso().get("055"))
                .setPin(payload.getMessageIso().get("052"))
                .setCardNumber(payload.getMessageIso().get("002"))
                .setCustomReturn(payload.getExecutionSimulationConfig().getCustomReturnSecurity())
                .setSleep(payload.getExecutionSimulationConfig().getSleepSecurity())
                .build();
    }
}
