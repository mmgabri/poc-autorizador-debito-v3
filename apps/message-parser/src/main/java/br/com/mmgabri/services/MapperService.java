package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import br.com.mmgabri.domains.AuthorizationRequest;
import br.com.mmgabri.domains.AuthorizationResponse;
import br.com.mmgabri.domains.HeaderMessage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MapperService {

    public AuthorizeTransactionRequest toAuthorizeTransactionRequest(AuthorizationRequest request, String correlationId) {
        var reversalTransactionId = request.getReversalTransactionId() != null ? request.getReversalTransactionId() : "";
        return AuthorizeTransactionRequest.newBuilder()
                .setCorrelationId(correlationId)
                .putAllMessageIso(request.getMessageIso())
                .setCustomReturnEnrichment(request.getCustomReturnEnrichment())
                .setSleepEnrichment(request.getSleepEnrichment())
                .setCustomReturnRules(request.getCustomReturnRules())
                .setSleepRules(request.getSleepRules())
                .setCustomReturnSecurity(request.getCustomReturnSecurity())
                .setSleepSecurity(request.getSleepSecurity())
                .setCustomReturnLimit(request.getCustomReturnLimit())
                .setSleepLimitCommit(request.getSleepLimitCommit())
                .setSleepLimitSimulation(request.getSleepLimitSimulation())
                .setCustomReturnAccountPosting(request.getCustomReturnAccountPosting())
                .setSleepAccountPostingCommit(request.getSleepAccountPostingCommit())
                .setSleepAccountPostingSimulation(request.getSleepAccountPostingSimulation())
                .setCustomReturnAntifraud(request.getCustomReturnAntifraud())
                .setSleepAntifraud(request.getSleepAntifraud())
                .setReversalTransactionId(reversalTransactionId)
                .build();
    }

    public AuthorizationResponse toReversalResponse(AuthorizationRequest request) {
        Map<String, String> iso = request.getMessageIso();
        Map<String, String> messageIso = new HashMap<>(iso);
        messageIso.put("039", "00");

        // Values below are part of the public REST response and are kept as-is.
        HeaderMessage header = HeaderMessage.builder()
                .transactionId(request.getReversalTransactionId())
                .correlationId(UUID.randomUUID().toString())
                .cardBrand("MASTERCARD")
                .platform("SINGLE_MESSAGE")
                .timestamp(LocalDateTime.now().toString())
                .message("Reversal solicitado com sucesso")
                .reversal(true)
                .build();

        return AuthorizationResponse.builder()
                .header(header)
                .messageIso(messageIso)
                .build();
    }

    /**
     * Header of the outgoing REST API (external contract, kept): the authorizer only
     * returns the transactionId; the transaction result travels in DE39 of messageIso.
     */
    public AuthorizationResponse toAuthorizationResponse(AuthorizeTransactionResponse response, String correlationId) {
        HeaderMessage header = HeaderMessage.builder()
                .transactionId(response.getTransactionId())
                .correlationId(correlationId)
                .cardBrand("Mastercard")
                .platform("SINGLE_MESSAGE")
                .timestamp(LocalDateTime.now().toString())
                .reversal(false)
                .build();

        return AuthorizationResponse.builder()
                .header(header)
                .messageIso(response.getMessageIsoMap())
                .build();
    }
}
