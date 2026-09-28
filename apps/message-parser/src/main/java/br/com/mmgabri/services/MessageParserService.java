package br.com.mmgabri.services;

import br.com.mmgabri.adapters.grpc.client.AuthorizerGrpcClient;
import br.com.mmgabri.domains.AuthorizationRequest;
import br.com.mmgabri.domains.AuthorizationResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class MessageParserService {
    private static final Logger logger = LoggerFactory.getLogger(MessageParserService.class);
    private static final Set<String> REVERSAL_MTIS = Set.of("0400", "0420");

    private final AuthorizerGrpcClient authorizerGrpcClient;
    private final ExecutorService vtExecutor;
    private final MapperService mapperService;
    private final MetricsService metricsService;
    private final ReversalServiceImpl reversalService;

    public CompletionStage<AuthorizationResponse> authorizeTransactionAsync(AuthorizationRequest request) {
        var startTime = OffsetDateTime.now();
        String mti = request.getMessageIso() != null ? request.getMessageIso().get("mti") : null;

        if (REVERSAL_MTIS.contains(mti)) {
            logger.debug("Reversal detected. mti={}", mti);
            reversalService.publishReversal(request);
            metricsService.incrementMetric("app_fmt_duration_reversal", startTime);
            logger.debug("Reversal published in {} ms.", Duration.between(startTime, OffsetDateTime.now()).toMillis());
            return CompletableFuture.completedFuture(mapperService.toReversalResponse(request));
        }

        return CompletableFuture.supplyAsync(() -> {
            // Generated here (entry point): it is the idempotency key of the transaction in the authorizer.
            var correlationId = UUID.randomUUID().toString();
            var grpcResponse = authorizerGrpcClient.execute(request, correlationId);
            var response = mapperService.toAuthorizationResponse(grpcResponse, correlationId);
            logger.info("Authorization completed in {} ms.", Duration.between(startTime, OffsetDateTime.now()).toMillis());
            metricsService.incrementMetric("app_fmt_duration_transaction", startTime);
            return response;
        }, vtExecutor);
    }
}
