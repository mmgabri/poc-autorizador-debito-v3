package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.dynamodb.mapper.DatabaseMapper;
import br.com.mmgabri.adapters.dynamodb.repository.ServiceContextRepository;
import br.com.mmgabri.adapters.dynamodb.repository.TransactionContextRepository;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.ServiceExecutionContext;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.ServiceAwareException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.*;

@Service
@RequiredArgsConstructor
public class TransactionContextRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(TransactionContextRegistryService.class);

    @Value("${custom.database-mode-async}")
    private boolean isDatabaseModeAsync;

    private final AsyncTaskExecutor asyncTaskExecutor;
    private final TransactionContextRepository repoTransaction;
    private final ServiceContextRepository repoService;
    private final DatabaseMapper databaseMapper;

    public TransactionExecutionContext initializeTransactionExecutionContext(Payload payload) {

        List<ServiceExecutionContext> services = new ArrayList<>();
        TransactionExecutionContext transaction = TransactionExecutionContext.builder()
                .transactionId(payload.getHeaderMessage().getTransactionId())
                .status(AuthorizationStatusEnum.IN_PROGRESS)
                .createdAt(LocalDateTime.now().toString())
                .services(services)
                .build();
        logger.debug("[Registry] Create transaction");
        return transaction;
    }

    public void updateStatusTransactionContext(TransactionExecutionContext transaction, AuthorizationStatusEnum status) {
        transaction.setStatus(status);
        logger.debug("[Registry] Update status transaction");
        registryTransactionContextDatabase(transaction);
    }


    public TransactionExecutionContext registerServiceExecution(TransactionExecutionContext transaction, ServicesEnum service, AuthorizationStatusEnum status) {
        if (!service.isHasCompensation()) {
            return transaction;
        }

        transaction.getServices().removeIf(s -> s.getService() == service);

        ServiceExecutionContext svc = ServiceExecutionContext.builder()
                .service(service)
                .status(status)
                .transactionId(transaction.getTransactionId())
                .executedAt(LocalDateTime.now().toString())
                .build();
        logger.debug("[Registry] Service approved {} ", service.getServiceName());
        transaction.getServices().add(svc);
        registryServiceContextDatabase(svc);
        return transaction;
    }

    public TransactionExecutionContext registerServiceExecution(TransactionExecutionContext transaction, ServiceAwareException exception) {
        ServicesEnum service = ServicesEnum.fromServiceName(exception.getService());
        ServiceExecutionContext svc = ServiceExecutionContext.builder()
                .service(service)
                .status(DENIED)
                .transactionId(transaction.getTransactionId())
                .errorCode(exception.getErrorCode())
                .errorDescription(exception.getErrorDescription())
                .executedAt(LocalDateTime.now().toString())
                .build();
        logger.debug("[Registry] Service denied {} ", service.getServiceName());
        transaction.getServices().add(svc);
        registryServiceContextDatabase(svc);
        return transaction;
    }

    private void registryTransactionContextDatabase(TransactionExecutionContext transaction) {

        if (!isDatabaseModeAsync) {
            repoTransaction.save(databaseMapper.mapToTransactionContext(transaction));
            return;
        }

        asyncTaskExecutor.execute(() -> {
            try {
                repoTransaction.save(databaseMapper.mapToTransactionContext(transaction));
            } catch (Exception e) {
                logger.error("[Registry] async saveTransaction failed. txId={}", transaction.getTransactionId(), e);
            }
        });
    }

    private void registryServiceContextDatabase(ServiceExecutionContext service) {

        if (!isDatabaseModeAsync) {
            repoService.save(databaseMapper.mapToServiceContext(service));
            return;
        }

        if (!service.getService().isRegistryAsync()) {
            repoService.save(databaseMapper.mapToServiceContext(service));
            return;
        }

        asyncTaskExecutor.execute(() -> {
            try {
                repoService.save(databaseMapper.mapToServiceContext(service));
            } catch (Exception e) {
                logger.error("[Registry] async saveService failed. txId={}", service.getTransactionId(), e);
            }
        });
    }
}