package br.com.mmgabri.services;

import br.com.mmgabri.errors.GrpcErrors;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityRequest;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityResponse;
import io.grpc.Status;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SecurityService {

    @Value("${custom.sleep:100}")
    long customSleepMillis;

    @SneakyThrows
    public ValidateSecurityResponse execute(ValidateSecurityRequest request) {

        if (request.getSleep() > 0) {
            sleep(Duration.ofMillis(request.getSleep()));
        } else {
            sleep(Duration.ofMillis(customSleepMillis));
        }

        var result = simulateResult(request.getCustomReturn());

        ValidateSecurityResponse response = ValidateSecurityResponse.newBuilder()
                .setResult(result)
                .setAccountId(request.getAccountId())
                .build();

        return response;

    }

    /**
     * Simulation driven by customReturn: empty/"000" approves, "999" simulates an
     * unhandled failure, and the catalog codes emitted by this service simulate a
     * decline (N*) or a technical failure (T*).
     */
    private BusinessResult simulateResult(String customReturn) {
        if (customReturn.isBlank() || "000".equals(customReturn)) {
            return BusinessResult.newBuilder().setApproved(true).build();
        }
        if ("999".equals(customReturn)) {
            throw new RuntimeException("Failure requested by the caller");
        }

        var reasonCode = GrpcErrors.fromCode(customReturn)
                .orElseThrow(() -> new IllegalArgumentException("customReturn not in the catalog: " + customReturn));

        return switch (reasonCode) {
            case REASON_CODE_INVALID_PIN,
                 REASON_CODE_CHIP_AUTHENTICATION_FAILED,
                 REASON_CODE_INVALID_CVV -> declined(reasonCode);
            case REASON_CODE_SECURITY_INTERNAL_ERROR -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.INTERNAL);
            case REASON_CODE_SECURITY_DEPENDENCY_UNAVAILABLE,
                 REASON_CODE_SECURITY_DEPENDENCY_TIMEOUT -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.UNAVAILABLE);
            default -> throw new IllegalArgumentException("customReturn not emitted by security: " + customReturn);
        };
    }

    private BusinessResult declined(ReasonCode reasonCode) {
        var description = GrpcErrors.descriptionOf(reasonCode);
        return BusinessResult.newBuilder()
                .setApproved(false)
                .setReasonCode(reasonCode)
                .setMessage(description)
                .build();
    }

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
