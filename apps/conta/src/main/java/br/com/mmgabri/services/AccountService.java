package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.common.v1.TechnicalError;
import br.com.mmgabri.adapters.grpc.client.LedgerServiceGrpcClient;
import br.com.mmgabri.domain.AccountCommand;
import br.com.mmgabri.errors.ReasonCodes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final LedgerServiceGrpcClient ledgerClient;

    public void execute(AccountCommand command) {

        sleep(Duration.ofMillis(command.sleepCommit()));

        var request = buildPostingResult(command);
        ledgerClient.execute(request);
    }

    /**
     * conta is the caller of the callback, so it cannot return a gRPC error:
     * a technical failure goes in the payload (TechnicalError) and account-posting
     * converts it into a gRPC error to the authorizer.
     */
    private HandlePostingResultRequest buildPostingResult(AccountCommand command) {
        var builder = HandlePostingResultRequest.newBuilder()
                .setCorrelationId(command.correlationId())
                .setInstanceId(command.instanceId());

        try {
            simulateOutcome(command.customReturn(), builder);
        } catch (RuntimeException e) {
            // Failure while processing in conta itself: notify account-posting through
            // the callback instead of leaving it waiting until the timeout.
            logger.error("Failed to process command. correlationId={}", command.correlationId(), e);
            var message = e.getMessage() == null ? "Internal error in conta" : e.getMessage();
            var technicalError = technicalError(ReasonCode.REASON_CODE_ACCOUNT_INTERNAL_ERROR, message);
            builder.setTechnicalError(technicalError);
        }

        return builder.build();
    }

    /**
     * Simulation driven by customReturn: empty/"000" approves, "999" simulates an
     * unhandled failure, and the catalog codes emitted by conta simulate a
     * decline (N*) or a technical failure (T*).
     */
    private void simulateOutcome(String customReturn, HandlePostingResultRequest.Builder builder) {
        if (customReturn == null || customReturn.isBlank() || "000".equals(customReturn)) {
            var approved = BusinessResult.newBuilder().setApproved(true).build();
            builder.setBusinessResult(approved);
            return;
        }
        if ("999".equals(customReturn)) {
            throw new RuntimeException("Failure requested by the caller");
        }

        var reasonCode = ReasonCodes.fromCode(customReturn)
                .orElseThrow(() -> new IllegalArgumentException("customReturn not in the catalog: " + customReturn));
        var description = ReasonCodes.descriptionOf(reasonCode);

        switch (reasonCode) {
            case REASON_CODE_INSUFFICIENT_FUNDS -> {
                var declined = BusinessResult.newBuilder()
                        .setApproved(false)
                        .setReasonCode(reasonCode)
                        .setMessage(description)
                        .build();
                builder.setBusinessResult(declined);
            }
            case REASON_CODE_ACCOUNT_INTERNAL_ERROR,
                 REASON_CODE_ACCOUNT_DEPENDENCY_UNAVAILABLE,
                 REASON_CODE_ACCOUNT_DEPENDENCY_TIMEOUT -> {
                var technicalError = technicalError(reasonCode, description);
                builder.setTechnicalError(technicalError);
            }
            default -> throw new IllegalArgumentException("customReturn not emitted by conta: " + customReturn);
        }
    }

    private TechnicalError technicalError(ReasonCode reasonCode, String message) {
        return TechnicalError.newBuilder()
                .setReasonCode(reasonCode)
                .setMessage(message)
                .build();
    }

    // Simulates processing time.
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
