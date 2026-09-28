package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.limit.v1.UpdateLimitRequest;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitResponse;
import br.com.mmgabri.errors.GrpcErrors;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import io.grpc.Status;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimitService {

    @SneakyThrows
    public UpdateLimitResponse execute(UpdateLimitRequest request) {


        if (request.getOperationType().equals("COMMIT")) {
            if (request.getSleepCommit() > 0) {
                sleep(Duration.ofMillis(request.getSleepCommit()));
            } else {
                sleep(Duration.ofMillis(1));
            }
        } else {
            if (request.getSleepSimulation() > 0) {
                sleep(Duration.ofMillis(request.getSleepSimulation()));
            } else {
                sleep(Duration.ofMillis(1));
            }
        }

        var result = resolveResult(request);

        UpdateLimitResponse response = UpdateLimitResponse.newBuilder()
                .setResult(result)
                .setAccountId(request.getAccountId())
                .build();

        return response;

    }

    /**
     * The simulated return only applies to COMMIT; SIMULATION always approves
     * (except "999", which simulates an unhandled failure in both).
     */
    private BusinessResult resolveResult(UpdateLimitRequest request) {
        if (request.getOperationType().equals("COMMIT")) {
            return simulateResult(request.getCustomReturn());
        }
        if ("999".equals(request.getCustomReturn())) {
            throw new RuntimeException("Failure requested by the caller");
        }
        return BusinessResult.newBuilder().setApproved(true).build();
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
            case REASON_CODE_LIMIT_EXCEEDED -> declined(reasonCode);
            case REASON_CODE_LIMIT_INTERNAL_ERROR -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.INTERNAL);
            case REASON_CODE_LIMIT_DEPENDENCY_UNAVAILABLE,
                 REASON_CODE_LIMIT_DEPENDENCY_TIMEOUT -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.UNAVAILABLE);
            default -> throw new IllegalArgumentException("customReturn not emitted by limit: " + customReturn);
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
