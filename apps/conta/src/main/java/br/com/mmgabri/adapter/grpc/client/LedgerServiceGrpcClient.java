package br.com.mmgabri.adapter.grpc.client;

import br.com.mmgabri.adapter.grpc.config.AsyncBridgeGrpcStubProvider;
import br.com.mmgabri.grpc.RetornoContaRequest;
import br.com.mmgabri.grpc.RetornoContaResponse;
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

    private final AsyncBridgeGrpcStubProvider asyncBridgeStubProvider;
    private final MetricsService metricsService;

    @SneakyThrows
    public void execute(RetornoContaRequest request) {
        try {
            logger.debug("Starting ledger-service callback call");
            RetornoContaResponse response = asyncBridgeStubProvider.getStub().trataRetornoConta(request);
            metricsService.incrementMetricCounter("app_conta_msg_send_ledger");
            logger.debug("ledger-service callback call succeeded. Message: {}", response.getMessage());
        } catch (Exception e) {
            logger.error("ledger-service callback call failed", e);
            throw e;
        }
    }
}
