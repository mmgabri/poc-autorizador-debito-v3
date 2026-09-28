package br.com.mmgabri.application.mappers;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichTransactionResponse;
import br.com.mmgabri.application.domains.*;
import org.springframework.stereotype.Service;

@Service
public class PayloadMapper {

    public Payload map(AuthorizeTransactionRequest request, ProductDomain product, String transactionId) {

        HeaderMessage header = HeaderMessage.builder()
                .transactionId(transactionId)
                .correlationId(request.getCorrelationId())
                .build();

        ExecutionSimulationConfig executionSimulationConfig = ExecutionSimulationConfig.builder()
                .customReturnEnrichment(request.getCustomReturnEnrichment())
                .sleepEnrichment(request.getSleepEnrichment())
                .customReturnRules(request.getCustomReturnRules())
                .sleepRules(request.getSleepRules())
                .customReturnSecurity(request.getCustomReturnSecurity())
                .sleepSecurity(request.getSleepSecurity())
                .customReturnLimit(request.getCustomReturnLimit())
                .sleepLimitCommit(request.getSleepLimitCommit())
                .sleepLimitSimulation(request.getSleepLimitSimulation())
                .customReturnAccountPosting(request.getCustomReturnAccountPosting())
                .sleepAccountPostingCommit(request.getSleepAccountPostingCommit())
                .sleepAccountPostingSimulation(request.getSleepAccountPostingSimulation())
                .customReturnAntifraud(request.getCustomReturnAntifraud())
                .sleepAntifraud(request.getSleepAntifraud())
                .reversalTransactionId(request.getReversalTransactionId())
                .build();

        return Payload.builder()
                .executionSimulationConfig(executionSimulationConfig)
                .headerMessage(header)
                .productDomain(product)
                .messageIso(request.getMessageIsoMap())
                .build();
    }

    public Payload mapEnrichedData(Payload payload, EnrichTransactionResponse enrichResponse) {

        CardDataDomain card = CardDataDomain.builder()
                .cardNumber(enrichResponse.getCard().getCardNumber())
                .cardHash(enrichResponse.getCard().getCardHash())
                .holderName(enrichResponse.getCard().getHolderName())
                .accountId(enrichResponse.getCard().getAccountId())
                .build();

        AccountDataDomain account = AccountDataDomain.builder()
                .accountId(enrichResponse.getAccount().getAccountId())
                .category(enrichResponse.getAccount().getCategory())
                .segment(enrichResponse.getAccount().getSegment())
                .type(enrichResponse.getAccount().getType())
                .build();

        CustomerDataDomain customer = CustomerDataDomain.builder()
                .customerId(enrichResponse.getCustomer().getCustomerId())
                .documentNumber(enrichResponse.getCustomer().getDocumentNumber())
                .phone(enrichResponse.getCustomer().getPhone())
                .build();

        TokenDataDomain token = TokenDataDomain.builder()
                .tokenId(enrichResponse.getToken().getTokenId())
                .status(enrichResponse.getToken().getStatus())
                .wallet(enrichResponse.getToken().getWallet())
                .build();

        DataEnrichmentDomain enrichedData = DataEnrichmentDomain.builder()
                .card(card)
                .account(account)
                .customer(customer)
                .token(token)
                .build();

        payload.setDataEnrichment(enrichedData);

        return payload;
    }
}
