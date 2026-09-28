package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.LedgerServiceGrpcStubProvider;
import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultResponse;
import br.com.mmgabri.services.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LedgerServiceGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(LedgerServiceGrpcClient.class);

    private final LedgerServiceGrpcStubProvider ledgerServiceGrpcStubProvider;
    private final MetricsService metricsService;

    @SneakyThrows
    public void execute(HandlePostingResultRequest request) {
        try {
            logger.debug("Starting account-posting callback call. correlationId={}", request.getCorrelationId());
            var stub = ledgerServiceGrpcStubProvider.getStub(request.getCorrelationId());
            HandlePostingResultResponse response = stub.handlePostingResult(request);
            metricsService.incrementMetricCounter("app_conta_msg_send_ledger");
            logger.debug("account-posting callback call succeeded. Message: {}", response.getMessage());
        } catch (Exception e) {
            logger.error("account-posting callback call failed", e);
            throw e;
        }
    }
}
