package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.enrichment.v1.*;

import br.com.mmgabri.errors.GrpcErrors;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import io.grpc.Status;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class EnrichmentService {

    @Value("${custom.sleep:100}")
    long customSleepMillis;

    @SneakyThrows
    public EnrichTransactionResponse execute(EnrichTransactionRequest request) {

        if (request.getSleep() > 0) {
            sleep(Duration.ofMillis(request.getSleep()));
        } else {
            sleep(Duration.ofMillis(1));
        }

        var result = simulateResult(request.getCustomReturn());

        CardData card = CardData.newBuilder()
                .setCardNumber(request.getCardNumber())
                .setCardHash("d9b06a17-8c45-4f1a-9b6f-f0b06a6d6b1c")
                .setAccountId("6f1c3f2e-1f4a-4b0e-9f7a-2c8e0a7d6c31")
                .setHolderName("1")
                .build();

        AccountData account = AccountData.newBuilder()
                .setAccountId("8a3d5c12-7b9e-4f6a-b2c1-3e9f0a4d7c58")
                .setCategory("698")
                .setSegment("L012")
                .setType("C")
                .build();

        CustomerData customer = CustomerData.newBuilder()
                .setCustomerId("2d7f4a9c-5c3b-4e1f-8a6d-9b0c1e2f3a47")
                .setDocumentNumber("2776914540")
                .setPhone("11995963448")
                .build();

        TokenData token = TokenData.newBuilder()
                .setTokenId("c4a1b8d2-3f6e-4a9c-8d1b-5e7f0a2c9b34")
                .setStatus("OK")
                .setWallet("963")
                .build();

        EnrichTransactionResponse response = EnrichTransactionResponse.newBuilder()
                .setResult(result)
                .setCard(card)
                .setCustomer(customer)
                .setAccount(account)
                .setToken(token)
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
            case REASON_CODE_INVALID_CARD -> declined(reasonCode);
            case REASON_CODE_ENRICHMENT_INTERNAL_ERROR -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.INTERNAL);
            case REASON_CODE_ENRICHMENT_DEPENDENCY_UNAVAILABLE,
                 REASON_CODE_ENRICHMENT_DEPENDENCY_TIMEOUT -> throw GrpcErrors.toStatusException(reasonCode, Status.Code.UNAVAILABLE);
            default -> throw new IllegalArgumentException("customReturn not emitted by enrichment: " + customReturn);
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
