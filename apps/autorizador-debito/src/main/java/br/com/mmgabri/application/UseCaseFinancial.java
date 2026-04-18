package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.*;
import br.com.mmgabri.adapters.grpc.mappers.AutorizadorMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.application.mappers.PayloadMapper;
import br.com.mmgabri.application.services.FraudesService;
import br.com.mmgabri.application.services.LimitePortadorService;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.grpc.AutorizadorResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.*;

@Service
@RequiredArgsConstructor
public class UseCaseFinancial {

    private static final Logger logger = LoggerFactory.getLogger(UseCaseFinancial.class);

    private final AutorizadorMapper autorizadorMapper;
    private final PayloadMapper payloadMapper;
    private final TransactionContextRegistryService transactionContextRegistry;
    private final UseCaseCompensation useCaseCompensation;
    private final DataEnrichmentGrpcClient dataEnrichmentGrpcClient;
    private final SegurancaGrpcClient segurancaGrpcClient;
    private final LimitePortadorGrpcClient limitePortadorGrpcClient;
    private final LimiteGrpcClient limiteGrpcClient;
    private final LancamentoContaGrpcClient lancamentoContaGrpcClient;
    private final FraudesService fraudesService;
    private final LimitePortadorService limitePortadorService;


    @SneakyThrows
    public AutorizadorResponse execute(Payload payload) {

        logger.info("Initiating use case execution for product '{}'", payload.getProductDomain().getProductName());

        var transactionExecutionContext = transactionContextRegistry.initializeTransactionExecutionContext(payload);

        try {
            for (ServicesEnum service : payload.getProductDomain().getServicesToExecute()) {
                if (service.isHasCompensation()) {
                    transactionExecutionContext = transactionContextRegistry.registerServiceExecution(transactionExecutionContext, service, PENDING);
                }
                switch (service) {
                    case DATA_ENRICHMENT:
                        var enrichByCardResponse = dataEnrichmentGrpcClient.execute(payload, false);
                        payload = payloadMapper.mapDadosEnriquecidos(payload, enrichByCardResponse);
                        transactionExecutionContext = transactionContextRegistry.updatePayloadTransactionContext(transactionExecutionContext, payload);
                        break;
                    case SEGURANCA:
                        segurancaGrpcClient.execute(payload, false);
                        break;
                    case LIMITE_PORTADOR:
                        if (limitePortadorService.shouldExecute(payload))
                            limitePortadorGrpcClient.execute(payload, false);
                        break;
                    case LIMITE:
                        limiteGrpcClient.execute(payload, false);
                        break;
                    case LANCAMENTO_CONTA:
                        lancamentoContaGrpcClient.execute(payload, false);
                        break;
                    case FRAUDES:
                        fraudesService.validarFraudes(payload);
                        break;
                    default:
                        throw new TechnicalException("usecase", "999", "service nao previsto");
                }
                transactionExecutionContext = transactionContextRegistry.registerServiceExecution(transactionExecutionContext, service, APPROVED);
            }

            return onCompleteTransactionApproved(transactionExecutionContext, payload);
        } catch (BusinessException e) {
            transactionContextRegistry.registerServiceExecution(transactionExecutionContext, e);
            return onCompleteTransactionDenied(transactionExecutionContext, payload, e);
        } catch (Exception e) {
            return onCompleteTransactionDenied(transactionExecutionContext, payload, e);
        }
    }

    private AutorizadorResponse onCompleteTransactionDenied(TransactionExecutionContext transaction, Payload payload, Throwable throwable) {
        logger.info("Use case completed - Transaction denied.");
        try {
            useCaseCompensation.execute(transaction, payload);
        } catch (Exception error) {
            logger.error("Transaction compensation failed.", error);
        }
        transactionContextRegistry.updateStatusTransactionContext(transaction, DENIED);
        return autorizadorMapper.toAutorizadorResponseError(payload, throwable);
    }

    private AutorizadorResponse onCompleteTransactionApproved(TransactionExecutionContext transaction, Payload payload) {
        logger.info("Use case completed - Transaction approved.");
        transactionContextRegistry.updateStatusTransactionContext(transaction, APPROVED);
        return autorizadorMapper.toAutorizadorResponseSuccess(payload);
    }
}