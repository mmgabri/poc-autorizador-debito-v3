package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.EnrichmentServiceGrpcClient;
import br.com.mmgabri.adapters.grpc.mappers.AuthorizationMapper;
import br.com.mmgabri.adapters.grpc.server.CorrelationServerInterceptor;
import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.mappers.PayloadMapper;
import br.com.mmgabri.application.services.GenerateTransactionIdService;
import br.com.mmgabri.application.services.IdempotencyService;
import br.com.mmgabri.application.services.TransactionSetupService;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessTransaction {

    private static final Logger logger = LoggerFactory.getLogger(ProcessTransaction.class);

    private final UseCaseAuthorization useCaseAuthorization;
    private final PayloadMapper payloadMapper;
    private final AuthorizationMapper authorizationMapper;
    private final TransactionSetupService transactionSetup;
    private final GenerateTransactionIdService generateTransactionId;
    private final IdempotencyService idempotencyService;
    private final EnrichmentServiceGrpcClient dataEnrichmentGrpcClient;

    public AuthorizeTransactionResponse execute(AuthorizeTransactionRequest request) {

        try {
            var product = transactionSetup.execute(request);
            return process(request, product);
        } catch (Exception error) {
            logger.error("Error processing transaction.", error);
            return authorizationMapper.toErrorResponse(request, error);
        }
    }

    @SneakyThrows
    private AuthorizeTransactionResponse process(AuthorizeTransactionRequest request, ProductDomain product) {
        var transactionId = generateTransactionId.generateFinancialTransactionId(request);
        var payload = payloadMapper.map(request, product, transactionId);
        // Removed by CorrelationServerInterceptor at the end of the gRPC callback.
        MDC.put(CorrelationServerInterceptor.TRANSACTION_ID_MDC, transactionId);
        idempotencyService.execute(payload);

        var enrichResponse = dataEnrichmentGrpcClient.execute(payload);
        payload = payloadMapper.mapEnrichedData(payload, enrichResponse);

        logger.debug("Enrichment completed. Forwarding to financial use case.");
        return useCaseAuthorization.execute(payload);
    }
}
