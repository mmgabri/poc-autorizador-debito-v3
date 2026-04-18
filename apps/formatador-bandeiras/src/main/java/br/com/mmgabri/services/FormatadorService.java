package br.com.mmgabri.services;

import br.com.mmgabri.adapters.grpc.client.AutorizadorGrpcClient;
import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.domains.FormatadorResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class FormatadorService {
    private static final Logger logger = LoggerFactory.getLogger(FormatadorService.class);

    private final AutorizadorGrpcClient autorizadorGrpcClient;
    private final ExecutorService vtExecutor;
    private final MapperService autorizadorMapper;
    private final MetricsService metricsService;

    public CompletionStage<FormatadorResponse> autorizarTransacaoAsync(FormatadorRequest request) {
        var startTime = OffsetDateTime.now();
        return CompletableFuture.supplyAsync(() -> {
            var grpcResponse = autorizadorGrpcClient.execute(request);
            var resp = autorizadorMapper.toFormatadorResponse(grpcResponse);
            logger.info("Authorization completed in {} ms.", Duration.between(startTime, OffsetDateTime.now()).toMillis());
            metricsService.incrementMetric("app_fmt_duration_transaction", startTime);
            return resp;
        }, vtExecutor);
    }
}
