package br.com.mmgabri.grpctest;

import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudRequest;
import br.com.itau.debit.authorizer.antifraud.v1.AnalyzeFraudResponse;
import br.com.itau.debit.authorizer.antifraud.v1.AntifraudServiceGrpc;
import br.com.itau.debit.authorizer.enrichment.v1.CardData;
import br.com.itau.debit.authorizer.enrichment.v1.CustomerData;
import br.com.itau.debit.authorizer.enrichment.v1.AccountData;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichmentServiceGrpc;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichTransactionRequest;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichTransactionResponse;
import br.com.itau.debit.authorizer.enrichment.v1.TokenData;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingResponse;
import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitRequest;
import br.com.itau.debit.authorizer.limit.v1.UpdateLimitResponse;
import br.com.itau.debit.authorizer.limit.v1.LimitServiceGrpc;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesRequest;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesResponse;
import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityRequest;
import br.com.itau.debit.authorizer.security.v1.ValidateSecurityResponse;
import br.com.itau.debit.authorizer.security.v1.SecurityServiceGrpc;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Fake gRPC servers (in-process) for the 6 dependencies of debit-authorizer.
 * Each one answers "approved" by default; tests replace the {@code responder} field
 * to simulate a decline, slowness (timeout) or an error of a specific dependency.
 * The call itself is still real gRPC (protobuf, deadline, status code) - only the
 * network is replaced by in-process.
 */
public class FakeDownstreamServices {

    private FakeDownstreamServices() {
    }

    private static BusinessResult approvedResult() {
        return BusinessResult.newBuilder().setApproved(true).build();
    }

    private static BusinessResult declinedResult(ReasonCode reasonCode, String description) {
        return BusinessResult.newBuilder()
                .setApproved(false)
                .setReasonCode(reasonCode)
                .setMessage(description)
                .build();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class Enrichment extends EnrichmentServiceGrpc.EnrichmentServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<EnrichTransactionRequest, EnrichTransactionResponse> responder = Enrichment::approved;

        public void reset() {
            callCount.set(0);
            responder = Enrichment::approved;
        }

        private static EnrichTransactionResponse approved(EnrichTransactionRequest req) {
            return EnrichTransactionResponse.newBuilder()
                    .setCard(CardData.newBuilder().setCardNumber(req.getCardNumber()).setAccountId("TEST-ACCOUNT").build())
                    .setAccount(AccountData.newBuilder().setAccountId("TEST-ACCOUNT").build())
                    .setCustomer(CustomerData.newBuilder().setCustomerId("TEST-CUSTOMER").build())
                    .setToken(TokenData.newBuilder().build())
                    .setResult(approvedResult())
                    .build();
        }

        @Override
        public void enrichTransaction(EnrichTransactionRequest request, StreamObserver<EnrichTransactionResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Rules extends RulesEngineServiceGrpc.RulesEngineServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<CheckRulesRequest, CheckRulesResponse> responder = Rules::approved;

        public void reset() {
            callCount.set(0);
            responder = Rules::approved;
        }

        private static CheckRulesResponse approved(CheckRulesRequest req) {
            return CheckRulesResponse.newBuilder().setResult(approvedResult()).build();
        }

        /** Waits {@code millis} before answering approved - simulates a slow/stuck dependency. */
        public void slow(long millis) {
            responder = req -> {
                sleep(millis);
                return approved(req);
            };
        }

        @Override
        public void checkRules(CheckRulesRequest request, StreamObserver<CheckRulesResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Security extends SecurityServiceGrpc.SecurityServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<ValidateSecurityRequest, ValidateSecurityResponse> responder = Security::approved;

        public void reset() {
            callCount.set(0);
            responder = Security::approved;
        }

        private static ValidateSecurityResponse approved(ValidateSecurityRequest req) {
            return ValidateSecurityResponse.newBuilder()
                    .setAccountId(req.getAccountId())
                    .setResult(approvedResult())
                    .build();
        }

        /** Declines every call from now on with the given code/description. */
        public void deny(ReasonCode reasonCode, String errorDescription) {
            responder = req -> ValidateSecurityResponse.newBuilder()
                    .setAccountId(req.getAccountId())
                    .setResult(declinedResult(reasonCode, errorDescription))
                    .build();
        }

        @Override
        public void validateSecurity(ValidateSecurityRequest request, StreamObserver<ValidateSecurityResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Limit extends LimitServiceGrpc.LimitServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<UpdateLimitRequest, UpdateLimitResponse> responder = Limit::approved;

        public void reset() {
            callCount.set(0);
            responder = Limit::approved;
        }

        private static UpdateLimitResponse approved(UpdateLimitRequest req) {
            return UpdateLimitResponse.newBuilder()
                    .setAccountId(req.getAccountId())
                    .setResult(approvedResult())
                    .build();
        }

        @Override
        public void updateLimit(UpdateLimitRequest request, StreamObserver<UpdateLimitResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Ledger extends AccountPostingServiceGrpc.AccountPostingServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<RequestPostingRequest, RequestPostingResponse> responder = Ledger::approved;

        public void reset() {
            callCount.set(0);
            responder = Ledger::approved;
        }

        private static RequestPostingResponse approved(RequestPostingRequest req) {
            return RequestPostingResponse.newBuilder()
                    .setAccountId(req.getAccountId())
                    .setResult(approvedResult())
                    .build();
        }

        /**
         * Approves the SIMULATION (phase 1) normally, but declines the COMMIT (phase 2) -
         * produces exactly the partial phase 2 failure scenario that triggers the saga.
         */
        public void denyCommit(ReasonCode reasonCode, String errorDescription) {
            responder = req -> {
                if (!"COMMIT".equals(req.getOperationType())) {
                    return approved(req);
                }
                return RequestPostingResponse.newBuilder()
                        .setAccountId(req.getAccountId())
                        .setResult(declinedResult(reasonCode, errorDescription))
                        .build();
            };
        }

        @Override
        public void requestPosting(RequestPostingRequest request, StreamObserver<RequestPostingResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class AntiFraud extends AntifraudServiceGrpc.AntifraudServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<AnalyzeFraudRequest, AnalyzeFraudResponse> responder = AntiFraud::approved;

        public void reset() {
            callCount.set(0);
            responder = AntiFraud::approved;
        }

        private static AnalyzeFraudResponse approved(AnalyzeFraudRequest req) {
            return AnalyzeFraudResponse.newBuilder().setResult(approvedResult()).build();
        }

        @Override
        public void analyzeFraud(AnalyzeFraudRequest request, StreamObserver<AnalyzeFraudResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }
}
