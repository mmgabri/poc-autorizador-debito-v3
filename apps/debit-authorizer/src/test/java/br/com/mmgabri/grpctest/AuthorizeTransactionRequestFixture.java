package br.com.mmgabri.grpctest;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;

import java.util.UUID;

/**
 * Builder of valid AuthorizationService requests, used by the integration tests.
 * Each static factory represents a product of the catalog (products_config.yml)
 * with the minimum ISO fields already filled in; tests only override what matters
 * for the scenario.
 */
public final class AuthorizeTransactionRequestFixture {

    private final AuthorizeTransactionRequest.Builder builder;

    private AuthorizeTransactionRequestFixture() {
        builder = AuthorizeTransactionRequest.newBuilder()
                .setCorrelationId(UUID.randomUUID().toString())
                .putMessageIso("mti", "0200") // -> product DOMESTIC_PURCHASE_CHIP_PIN_MASTER
                .putMessageIso("002", "5555666677778888") // card number
                .putMessageIso("004", "000000010000")     // transaction amount
                .putMessageIso("052", "1234")             // PIN
                .putMessageIso("055", "CHIPDATA");        // chip data

    }

    /** Domestic purchase with chip + PIN, default product used by most scenarios. */
    public static AuthorizeTransactionRequestFixture domesticPurchaseWithChip() {
        return new AuthorizeTransactionRequestFixture();
    }

    public AuthorizeTransactionRequestFixture withCorrelationId(String correlationId) {
        builder.setCorrelationId(correlationId);
        return this;
    }

    public AuthorizeTransactionRequestFixture withAmount(String amount) {
        builder.putMessageIso("004", amount);
        return this;
    }

    public AuthorizeTransactionRequest build() {
        return builder.build();
    }
}
