package br.com.mmgabri.adapters.rest;

import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.domains.FormatadorResponse;
import br.com.mmgabri.services.FormatadorService;
import br.com.mmgabri.services.MetricsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletionStage;

@RestController
@RequiredArgsConstructor
public class ControllerFormatador {
    private static final Logger logger = LoggerFactory.getLogger(ControllerFormatador.class);
    private final FormatadorService formatadorService;
    private final MetricsService metricsService;

    @PostMapping("/authorization")
    public CompletionStage<ResponseEntity<FormatadorResponse>> authorization(@RequestBody FormatadorRequest request) {
        logger.debug("Starting authorization processing");

        return formatadorService.autorizarTransacaoAsync(request)
                .thenApply(ResponseEntity::ok);
    }
}