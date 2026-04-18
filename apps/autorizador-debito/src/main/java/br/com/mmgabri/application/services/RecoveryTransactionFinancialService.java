package br.com.mmgabri.application.services;

import br.com.mmgabri.adapters.database.dynamodb.entity.ServiceContextEntity;
import br.com.mmgabri.adapters.database.dynamodb.entity.TransactionContextEntity;
import br.com.mmgabri.adapters.database.dynamodb.repository.ServiceContextRepository;
import br.com.mmgabri.adapters.database.dynamodb.repository.TransactionContextRepository;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.domains.ServiceExecutionContext;
import br.com.mmgabri.application.domains.TransactionExecutionContext;
import br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum;
import br.com.mmgabri.application.domains.enuns.ServicesEnum;
import br.com.mmgabri.application.exceptions.BusinessException;
import br.com.mmgabri.application.utils.JsonConverter;
import br.com.mmgabri.grpc.AutorizadorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static br.com.mmgabri.application.domains.enuns.AuthorizationStatusEnum.APPROVED;

@Service
@RequiredArgsConstructor
public class RecoveryTransactionFinancialService {

    private final TransactionContextRepository transactionRepo;
    private final ServiceContextRepository serviceRepo;
    private final JsonConverter jsonConverter;
    private final GenerateTransactionIdService generateTransactionId;

    public Payload execute (AutorizadorRequest request) {
        var transactionId = generateTransactionId.generateTransacionIdReversal(request);

        var transactionContextDb = transactionRepo.getByTransactionId(transactionId);
        var servicesContextDb = serviceRepo.getServicesByTransactionId(transactionId);

        if (!transactionContextDb.getStatus().equals(APPROVED.toString())) {
            throw new BusinessException("recoveryTransaction", "999", "Transação não pode ser estornada");
        }

        var payloadFinancial = jsonConverter.jsonToObj(transactionContextDb.getPayload());

        Payload payload = Payload.builder()
                .headerMessage(payloadFinancial.getHeaderMessage())
                .messageIso(payloadFinancial.getMessageIso())
                .productDomain(payloadFinancial.getProductDomain())
                .dataEnrichment(payloadFinancial.getDataEnrichment())
                .executionSimulationConfig(payloadFinancial.getExecutionSimulationConfig())
                .transactionExecutionContextOrigin(getTransactionExecutionContext(transactionContextDb, servicesContextDb))
                .build();

        return payload;
    }

    private TransactionExecutionContext getTransactionExecutionContext(TransactionContextEntity transactionContextDb, List<ServiceContextEntity> servicesContextDb) {
        List<ServiceExecutionContext> serviceExecutionContexts = new ArrayList<>();

        for (ServiceContextEntity service : servicesContextDb) {
            ServiceExecutionContext serviceContext = ServiceExecutionContext.builder()
                    .transactionId(service.getTransactionId())
                    .service(ServicesEnum.fromServiceName(service.getServiceName()))
                    .status(AuthorizationStatusEnum.valueOf(service.getStatus()))
                    .errorCode(service.getErrorCode())
                    .errorDescription(service.getErrorDescription())
                    .executedAt(service.getExecutedAt())
                    .reversedAt(service.getReversedAt())
                    .build();
            serviceExecutionContexts.add(serviceContext);
        }

        TransactionExecutionContext transactionContext = TransactionExecutionContext.builder()
                .status(AuthorizationStatusEnum.valueOf(transactionContextDb.getStatus()))
                .transactionId(transactionContextDb.getTransactionId())
                .payload(jsonConverter.jsonToObj(transactionContextDb.getPayload()))
                .createdAt(transactionContextDb.getCreatedAt())
                .reversedAt(transactionContextDb.getReversedAt())
                .services(serviceExecutionContexts)
                .build();

        return transactionContext;
    }
}
