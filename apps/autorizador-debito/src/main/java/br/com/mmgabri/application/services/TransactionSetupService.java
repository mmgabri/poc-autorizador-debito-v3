package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.domains.enuns.TransactionOperationEnum;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.grpc.AutorizadorRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionSetupService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionSetupService.class);

    private final ProductCatalogService productCatalogService;

    public ProductDomain execute(AutorizadorRequest request) {
        var productName = discoveryProduct(request);
        return getConfigProduct(productName);
    }

    public String discoveryProduct(AutorizadorRequest request) {

        if (request.getMessageIsoMap().get("mti").equals("0200") || request.getMessageIsoMap().get("mti").equals("0100")) {
            return "COMPRA_NACIONAL_COM_CHIP_SENHA_MASTER";
        }

        if (request.getMessageIsoMap().get("mti").equals("0220") || request.getMessageIsoMap().get("mti").equals("0120")) {
            return "ADVICE_AUTHORIZATION";
        }

        logger.error("Could not identify the product.");
        throw new TechnicalException("identifyProduct", "999", "produto não identificado");
    }

    public ProductDomain getConfigProduct(String productName) {
        var productConfig = productCatalogService.findByProductName(productName);

        return ProductDomain.builder()
                .productName(productName)
                .codigoLiteral(productConfig.getCodigoLiteral())
                .enabled(productConfig.isEnabled())
                .transactionOperation(TransactionOperationEnum.fromOperationName(productConfig.getOperation()))
                .configSeguranca(productConfig.getConfigSeguranca())
                .roteiroContabil(productConfig.getRoteiroContabil())
                .build();
    }
}