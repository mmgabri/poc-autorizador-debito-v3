package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.domains.enums.TransactionOperationEnum;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionSetupService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionSetupService.class);

    private final ProductCatalogService productCatalogService;

    public ProductDomain execute(AuthorizeTransactionRequest request) {
        var productName = discoveryProduct(request);
        return getConfigProduct(productName);
    }

    public String discoveryProduct(AuthorizeTransactionRequest request) {

        if (request.getMessageIsoMap().get("mti").equals("0200") || request.getMessageIsoMap().get("mti").equals("0100")) {
            return "DOMESTIC_PURCHASE_CHIP_PIN_MASTER";
        }

        if (request.getMessageIsoMap().get("mti").equals("0220") || request.getMessageIsoMap().get("mti").equals("0120")) {
            return "ADVICE_AUTHORIZATION";
        }

        logger.error("Could not identify the product.");
        throw new TechnicalException("identifyProduct", ReasonCode.REASON_CODE_AUTHORIZER_PRODUCT_NOT_IDENTIFIED, "product not identified");
    }

    public ProductDomain getConfigProduct(String productName) {
        var productConfig = productCatalogService.findByProductName(productName);

        return ProductDomain.builder()
                .productName(productName)
                .descriptionCode(productConfig.getDescriptionCode())
                .enabled(productConfig.isEnabled())
                .transactionOperation(TransactionOperationEnum.fromOperationName(productConfig.getOperation()))
                .securityChecks(productConfig.getSecurityChecks())
                .accountingScheme(productConfig.getAccountingScheme())
                .build();
    }
}