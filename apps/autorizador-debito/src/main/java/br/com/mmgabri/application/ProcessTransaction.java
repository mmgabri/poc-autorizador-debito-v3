package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.mappers.AutorizadorMapper;
import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.mappers.PayloadMapper;
import br.com.mmgabri.application.services.GenerateTransactionIdService;
import br.com.mmgabri.application.services.IdempotencyService;
import br.com.mmgabri.application.services.RecoveryTransactionFinancialService;
import br.com.mmgabri.application.services.TransactionSetupService;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.AutorizadorResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessTransaction {

    private static final Logger logger = LoggerFactory.getLogger(ProcessTransaction.class);

    private final UseCaseFinancial useCaseFinancial;
    private final UseCaseReversal useCaseReversal;
    private final PayloadMapper payloadMapper;
    private final AutorizadorMapper autorizadorMapper;
    private final TransactionSetupService transactionSetup;
    private final GenerateTransactionIdService generateTransactionId;
    private final RecoveryTransactionFinancialService recoveryTransactionFinancial;
    private final IdempotencyService idempotencyService;


    public AutorizadorResponse process(AutorizadorRequest request) {

        try {
            var product = transactionSetup.execute(request);

            return switch (product.getTransactionOperation()) {
                case DEBITO, CREDITO -> processTransactionFinancial(request, product);
                case ESTORNO -> processTransactionReversal(request);
                default -> {
                    logger.error("Operation not supported. {}", product.getTransactionOperation());
                    //TODO - Checar se precisa de compensação
                    yield autorizadorMapper.toAutorizadorResponseError(request, new Throwable("Operation not supported"));
                }
            };

        } catch (Exception error) {
            logger.error("Error processing transaction.", error);
            return autorizadorMapper.toAutorizadorResponseError(request, error);
        }
    }

    @SneakyThrows
    private AutorizadorResponse processTransactionFinancial(AutorizadorRequest request, ProductDomain product) {
        var payload = payloadMapper.map(request, product, generateTransactionId.generateTransacionIdFinancial(request));
        idempotencyService.execute(payload);
        var response = useCaseFinancial.execute(payload);
        logger.info("Financial transaction processed successfully.");
        return response;
    }

    @SneakyThrows
    private AutorizadorResponse processTransactionReversal(AutorizadorRequest request) {
        var payloadFinancial = recoveryTransactionFinancial.execute(request);
        var payloadReversal = payloadMapper.map(request, payloadFinancial.getProductDomain(), payloadFinancial.getHeaderMessage().getTransactionId());
        var response = useCaseReversal.execute(payloadFinancial, payloadReversal);
        return response;
    }
}