package br.com.mmgabri.adapters.rest;

import br.com.mmgabri.domains.AuthorizationRequest;
import br.com.mmgabri.domains.AuthorizationResponse;
import br.com.mmgabri.services.MessageParserService;
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
public class MessageParserController {
    private static final Logger logger = LoggerFactory.getLogger(MessageParserController.class);
    private final MessageParserService messageParserService;

    @PostMapping("/authorization")
    public CompletionStage<ResponseEntity<AuthorizationResponse>> authorization(@RequestBody AuthorizationRequest request) {
        logger.debug("Starting authorization processing");

        return messageParserService.authorizeTransactionAsync(request)
                .thenApply(ResponseEntity::ok);
    }
}
