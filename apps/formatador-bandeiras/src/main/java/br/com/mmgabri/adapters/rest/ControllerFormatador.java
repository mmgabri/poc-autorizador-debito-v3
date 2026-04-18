package br.com.mmgabri.adapters.rest;

import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.domains.FormatadorResponse;
import br.com.mmgabri.domains.ReversalRequest;
import br.com.mmgabri.services.FormatadorService;
import br.com.mmgabri.services.MetricsService;
import br.com.mmgabri.services.ReversalService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletionStage;

@RestController
@RequiredArgsConstructor
public class ControllerFormatador {
    private static final Logger logger = LoggerFactory.getLogger(ControllerFormatador.class);
    private final FormatadorService formatadorService;
    private final MetricsService metricsService;
    private final ReversalService reversalService;

    @PostMapping("/authorization")
    public CompletionStage<ResponseEntity<FormatadorResponse>> formatador(@RequestBody FormatadorRequest request) {
        logger.info("Starting authorization processing");

        return formatadorService.autorizarTransacaoAsync(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/reversal")
    public ResponseEntity<Map<String, String>> reversal(@RequestBody ReversalRequest request) {
        logger.info("Starting reversal processing. transactionId={}", request.getTransactionId());
        reversalService.publishReversal(request);
        return ResponseEntity.ok(Map.of("status", "ACCEPTED", "message", "Reversal enviado para processamento"));
    }
}
