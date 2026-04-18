package br.com.mmgabri.adapters.database.dynamodb.mapper;

import br.com.mmgabri.adapters.database.dynamodb.entity.IdempotencyEntity;
import br.com.mmgabri.adapters.database.dynamodb.entity.ServiceContextEntity;
import br.com.mmgabri.adapters.database.dynamodb.entity.TransactionContextEntity;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.ServiceExecutionContext;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.utils.JsonConverter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DatabaseMapper {
    @Value("${custom.database-memo}")
    private boolean isDatabaseMemo;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMapper.class);
    private final JsonConverter jsonConverter;

    public TransactionContextEntity mapToTransactionContext(TransactionExecutionContext transaction) {
        var tran = new TransactionContextEntity();
        tran.setTransactionId(transaction.getTransactionId());
        tran.setStatus(transaction.getStatus().toString());
        tran.setPayload(jsonConverter.objToJson(transaction.getPayload()));
        tran.setCreatedAt(transaction.getCreatedAt());
        tran.setReversedAt(transaction.getReversedAt());
        return tran;
    }

    public ServiceContextEntity mapToServiceContext(ServiceExecutionContext service) {
        var svc = new ServiceContextEntity();
        svc.setTransactionId(service.getTransactionId());
        svc.setServiceName(service.getService().getServiceName());
        svc.setStatus(service.getStatus().toString());
        svc.setErrorCode(service.getErrorCode());
        svc.setErrorDescription(service.getErrorDescription());
        svc.setExecutedAt(service.getExecutedAt());
        svc.setReversedAt(service.getReversedAt());
        return svc;

    }

    public IdempotencyEntity mapToIdempotency(Payload payload) {
        var idempotency = new IdempotencyEntity();
        idempotency.setCorrelationId(payload.getHeaderMessage().getCorrelationId());
        idempotency.setTransactionId(payload.getHeaderMessage().getTransactionId());
        idempotency.setExecutedAt(LocalDateTime.now().toString());
        return idempotency;

    }
}
