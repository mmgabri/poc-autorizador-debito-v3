package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.domains.enuns.TransactionOperationEnum;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.grpc.AutorizadorRequest;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
            // Simulating product COMPRA_NACIONAL_COM_CHIP_SENHA_MASTER
            return ("COMPRA_NACIONAL_COM_CHIP_SENHA_MASTER");
        }

        if (request.getMessageIsoMap().get("mti").equals("0420") || request.getMessageIsoMap().get("mti").equals("0400")) {
            return ("ESTORNO");
        }

        if (request.getMessageIsoMap().get("mti").equals("0220") || request.getMessageIsoMap().get("mti").equals("0120")) {
            // Simulating product ADVICE
            return ("ADVICE_AUTHORIZATION");
        }

        logger.error("Could not identify the product.");
        throw new TechnicalException("identifyProduct", "999", "produto não identificado");
    }


    public ProductDomain getConfigProduct(String productName) {

        if (productName.equals("ESTORNO")) {
            return ProductDomain.builder()
                    .transactionOperation(TransactionOperationEnum.fromOperationName("ESTORNO"))
                    .build();
        }

        var productConfig = productCatalogService.findByProductName(productName);

        return ProductDomain.builder()
                .productName(productName)
                .codigoLiteral(productConfig.getCodigoLiteral())
                .enabled(productConfig.isEnabled())
                .transactionOperation(TransactionOperationEnum.fromOperationName(productConfig.getOperation()))
                .configSeguranca(productConfig.getConfigSeguranca())
                .roteiroContabil(productConfig.getRoteiroContabil())
                .servicesToExecute(setServicesToExecuteFromStrings(productConfig.getServicesToExecute()))
                .build();
    }

    private List<ServicesEnum> setServicesToExecuteFromStrings(List<String> rawCalls) {

        List<ServicesEnum> servicesToExecute = new ArrayList<>();

        for (String s : rawCalls) {
            if (s == null || s.isBlank()) continue;
            servicesToExecute.add(ServicesEnum.fromServiceName(s.trim()));
        }

        return servicesToExecute;
    }
}