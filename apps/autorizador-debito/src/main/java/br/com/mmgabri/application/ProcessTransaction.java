package br.com.mmgabri.application;

import br.com.mmgabri.adapters.grpc.client.EnrichmentServiceGrpcClient;
import br.com.mmgabri.adapters.grpc.mappers.AutorizadorMapper;
import br.com.mmgabri.application.domains.ProductDomain;
import br.com.mmgabri.application.mappers.PayloadMapper;
import br.com.mmgabri.application.services.GenerateTransactionIdService;
import br.com.mmgabri.application.services.IdempotencyService;
import br.com.mmgabri.application.services.TransactionSetupService;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorRequest;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcessTransaction {

    private static final Logger logger = LoggerFactory.getLogger(ProcessTransaction.class);

    private final UseCaseAuthorization useCaseAuthorization;
    private final PayloadMapper payloadMapper;
    private final AutorizadorMapper autorizadorMapper;
    private final TransactionSetupService transactionSetup;
    private final GenerateTransactionIdService generateTransactionId;
    private final IdempotencyService idempotencyService;
    private final EnrichmentServiceGrpcClient dataEnrichmentGrpcClient;

    public AutorizadorResponse execute(AutorizadorRequest request) {

        try {
            var product = transactionSetup.execute(request);
            return process(request, product);
        } catch (Exception error) {
            logger.error("Error processing transaction.", error);
            return autorizadorMapper.toAutorizadorResponseError(request, error);
        }
    }

    @SneakyThrows
    private AutorizadorResponse process(AutorizadorRequest request, ProductDomain product) {
        var payload = payloadMapper.map(request, product, generateTransactionId.generateTransacionIdFinancial(request));
        idempotencyService.execute(payload);

        var enrichResponse = dataEnrichmentGrpcClient.execute(payload);
        payload = payloadMapper.mapEnrichedData(payload, enrichResponse);

        logger.debug("Enrichment completed. Forwarding to financial use case.");
        return useCaseAuthorization.execute(payload);
    }
}
