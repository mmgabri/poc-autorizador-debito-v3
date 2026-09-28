package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingResponse;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class LedgerSimulationService {

    private static final Logger logger = LoggerFactory.getLogger(LedgerSimulationService.class);

    @SneakyThrows
    public RequestPostingResponse execute(RequestPostingRequest request) {
        String correlationId = request.getCorrelationId();
        logger.debug("Simulation started. correlationId={}", correlationId);

        int sleep = request.getSleepSimulation();
        if (sleep > 0) {
            TimeUnit.MILLISECONDS.sleep(sleep);
        }

        logger.debug("Simulation completed without calling conta. correlationId={}", correlationId);

        return RequestPostingResponse.newBuilder()
                .setResult(BusinessResult.newBuilder().setApproved(true).build())
                .setAccountId(request.getAccountId())
                .build();
    }
}
