package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.LancamentoContaGrpcClient;
import br.com.mmgabri.adapters.grpc.client.LimiteGrpcClient;
import br.com.mmgabri.adapters.grpc.client.LimitePortadorGrpcClient;
import br.com.mmgabri.adapters.grpc.mappers.AutorizadorMapper;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.ServiceExecutionContext;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import br.com.mmgabri.grpc.AutorizadorResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.*;

@Service
@RequiredArgsConstructor
public class UseCaseReversal {
    private static final Logger logger = LoggerFactory.getLogger(UseCaseReversal.class);

    private final TransactionContextRegistryService transactionContextRegistry;
    private final LimitePortadorGrpcClient limitePortadorGrpcClient;
    private final LimiteGrpcClient limiteGrpcClient;
    private final LancamentoContaGrpcClient lancamentoContaGrpcClient;
    private final AutorizadorMapper autorizadorMapper;


    public AutorizadorResponse execute(Payload payloadFinancial, Payload payloadReversal) {

        if (!shouldReversed(payloadFinancial.getTransactionExecutionContextOrigin())) {
            throw new TechnicalException("usecaseReversal", "999", "Não há serviços para serem revertidos");
        }

        var transactionExecutionContext = payloadFinancial.getTransactionExecutionContextOrigin();
        transactionExecutionContext.setStatus(IN_REVERSAL);
        transactionContextRegistry.updateStatusTransactionContext(transactionExecutionContext, IN_REVERSAL);

        logger.info("Starting reversal.");

        try {
            for (ServiceExecutionContext svc : transactionExecutionContext.getServices()) {

                if (svc.getStatus() != APPROVED || svc.getService() == null || !svc.getService().isHasCompensation()) {
                    continue;
                }

                ServicesEnum service = svc.getService();

                switch (service) {
                    case LIMITE_PORTADOR:
                        limitePortadorGrpcClient.execute(payloadFinancial, true);
                        break;
                    case LIMITE:
                        limiteGrpcClient.execute(payloadFinancial, true);
                        break;
                    case LANCAMENTO_CONTA:
                        lancamentoContaGrpcClient.execute(payloadFinancial, true);
                        break;
                    default:
                        throw new TechnicalException("usecaseReversal", "999", "service nao previsto");
                }
                transactionExecutionContext = transactionContextRegistry.markServiceExecutionReversed(transactionExecutionContext, service);
            }
            return onReversalCompleteTransactionApproved(transactionExecutionContext, payloadReversal);

        } catch (BusinessException e) {
            logger.error("Compensation failed.", e);
            transactionContextRegistry.markServiceExecutionReversed(transactionExecutionContext, e);
            return onReversalCompleteTransactionDenied(transactionExecutionContext, payloadReversal, e);
        } catch (Exception e) {
            logger.error("Compensation failed.", e);
            return onReversalCompleteTransactionDenied(transactionExecutionContext, payloadReversal, e);
        }
    }

    private AutorizadorResponse onReversalCompleteTransactionDenied(TransactionExecutionContext transaction, Payload payload, Throwable throwable) {
        logger.info("Use case reversal completed - Transaction denied.");
        transactionContextRegistry.updateStatusTransactionContext(transaction, ERROR_REVERSED);
        return autorizadorMapper.toAutorizadorResponseError(payload, throwable);
    }

    private AutorizadorResponse onReversalCompleteTransactionApproved(TransactionExecutionContext transaction, Payload payload) {
        logger.info("Use case reversal completed - Transaction approved.");
        transactionContextRegistry.updateStatusTransactionContext(transaction, REVERSED);
        return autorizadorMapper.toAutorizadorResponseSuccess(payload);
    }

    private boolean shouldReversed(TransactionExecutionContext transaction) {
        return transaction.getServices().stream()
                .anyMatch(s ->
                        s.getStatus() == APPROVED &&
                                s.getService() != null &&
                                s.getService().isHasCompensation()
                );
    }
}