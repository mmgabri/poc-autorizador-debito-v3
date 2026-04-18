package br.com.mmgabri.adapters.database.postgreSQL.mapper;

import br.com.mmgabri.adapters.database.postgreSQL.entity.IdempotencyEntityH2;
import br.com.mmgabri.adapters.database.postgreSQL.entity.ServiceContextEntityH2;
import br.com.mmgabri.adapters.database.postgreSQL.entity.ServiceIdH2;
import br.com.mmgabri.adapters.database.postgreSQL.entity.TransactionContextEntityH2;
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
public class DatabaseH2Mapper {
    @Value("${custom.database-memo}")
    private boolean isDatabaseMemo;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseH2Mapper.class);
    private final JsonConverter jsonConverter;

    public TransactionContextEntityH2 mapToTransactionContext(TransactionExecutionContext transaction) {
        return TransactionContextEntityH2.builder()
                .transactionId(transaction.getTransactionId())
                .status(transaction.getStatus().toString())
                .payload(jsonConverter.objToJson(transaction.getPayload()))
                .createdAt(transaction.getCreatedAt())
                .reversedAt(transaction.getReversedAt())
                .build();
    }

    public ServiceContextEntityH2 mapToServiceContext(ServiceExecutionContext service) {
        return ServiceContextEntityH2.builder()
                .serviceId(new ServiceIdH2(service.getTransactionId(), service.getService().getServiceName()))
                .status(service.getStatus().toString())
                .errorCode(service.getErrorCode())
                .errorDescription(service.getErrorDescription())
                .executedAt(service.getExecutedAt())
                .reversedAt(service.getReversedAt())
                .build();
    }

    public IdempotencyEntityH2 mapToIdempotency(Payload payload) {
        return IdempotencyEntityH2.builder()
                .transactionId(payload.getHeaderMessage().getTransactionId())
                .correlationId(payload.getHeaderMessage().getCorrelationId())
                .executedAt(LocalDateTime.now().toString())
                .build();
    }
}
