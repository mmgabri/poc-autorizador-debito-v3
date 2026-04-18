package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.LancamentoContaGrpcClient;
import br.com.mmgabri.adapters.grpc.client.LimiteGrpcClient;
import br.com.mmgabri.adapters.grpc.client.LimitePortadorGrpcClient;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.ServiceExecutionContext;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.exceptions.TechnicalException;
import br.com.mmgabri.application.services.TransactionContextRegistryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.APPROVED;
import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.IN_COMPENSATION;

@Service
@RequiredArgsConstructor
public class UseCaseCompensation {
    private static final Logger logger = LoggerFactory.getLogger(UseCaseCompensation.class);

    private final TransactionContextRegistryService transactionContextRegistry;
    private final LimitePortadorGrpcClient limitePortadorGrpcClient;
    private final LimiteGrpcClient limiteGrpcClient;
    private final LancamentoContaGrpcClient lancamentoContaGrpcClient;


    public void execute(TransactionExecutionContext transactionExecutionContext, Payload payload) {

        if (!shouldCompensate(transactionExecutionContext)) {
            logger.info("No services to compensate.");
            return;
        }

        logger.info("Starting compensation.");

        transactionExecutionContext.setStatus(IN_COMPENSATION);
        transactionContextRegistry.updateStatusTransactionContext(transactionExecutionContext, IN_COMPENSATION);

        try {
            for (ServiceExecutionContext svc : transactionExecutionContext.getServices()) {

                if (svc.getStatus() != APPROVED || svc.getService() == null || !svc.getService().isHasCompensation()) {
                    continue;
                }

                ServicesEnum service = svc.getService();

                switch (service) {
                    case LIMITE_PORTADOR:
                        limitePortadorGrpcClient.execute(payload, true);
                        break;
                    case LIMITE:
                        limiteGrpcClient.execute(payload, true);
                        break;
                    case LANCAMENTO_CONTA:
                        lancamentoContaGrpcClient.execute(payload, true);
                        break;
                    default:
                        throw new TechnicalException("usecase", "999", "service nao previsto");
                }
                transactionExecutionContext = transactionContextRegistry.markServiceExecutionReversed(transactionExecutionContext, service);
            }

            logger.info("Compensation completed successfully.");

        } catch (BusinessException e) {
            logger.error("Compensation failed.", e);
            transactionContextRegistry.markServiceExecutionReversed(transactionExecutionContext, e);
            throw e;
        } catch (Exception e) {
            logger.error("Compensation failed.", e);
            throw e;
        }

    }

    private boolean shouldCompensate(TransactionExecutionContext transaction) {
        return transaction.getServices().stream()
                .anyMatch(s ->
                        s.getStatus() == APPROVED &&
                                s.getService() != null &&
                                s.getService().isHasCompensation()
                );
    }
}