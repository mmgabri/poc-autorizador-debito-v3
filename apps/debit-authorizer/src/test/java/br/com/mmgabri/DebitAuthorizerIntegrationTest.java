package br.com.mmgabri;

import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizationServiceGrpc;
import br.com.mmgabri.awstest.LocalStackTestConfig;
import br.com.mmgabri.grpctest.AuthorizeTransactionRequestFixture;
import br.com.mmgabri.grpctest.FakeDownstreamServices;
import br.com.mmgabri.grpctest.GrpcInProcessTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration test of debit-authorizer: enters through real gRPC
 * (in-process) in AuthorizationGrpcServer, goes through the whole real orchestration
 * (ProcessTransaction / UseCaseAuthorization, including the parallel Phase 1/Phase 2
 * calls), and leaves through real gRPC (in-process) to the 6 dependencies, whose
 * behavior is controlled by the fakes in {@link FakeDownstreamServices}.
 * DynamoDB (idempotency/context) and SQS (compensation) are real, against a
 * LocalStack container (see {@link LocalStackTestConfig}) - not mocks, because
 * pool/timeout/conditional-write bugs only show up against the real SDK.
 */
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.grpc.server.port=0",
                "management.datadog.metrics.export.enabled=false",
                // The real dependency timeouts (application.yml) are 1h - fine for local
                // debugging, unusable to test a timeout. Shortened only here.
                "grpc.enrichment-client.timeout=2000",
                "grpc.rules-engine-client.timeout=2000",
                "grpc.security-client.timeout=2000",
                "grpc.limit-client.timeout=2000",
                "grpc.account-posting-client.timeout=2000",
                "grpc.antifraud-client.timeout=2000"
        })
@Import({GrpcInProcessTestConfig.class, LocalStackTestConfig.class})
class DebitAuthorizerIntegrationTest {

    private static final String STATUS_APPROVED = "00";
    private static final String STATUS_DECLINED = "96";

    @Autowired
    private AuthorizationServiceGrpc.AuthorizationServiceBlockingV2Stub authorizerTestClient;

    @Autowired
    private FakeDownstreamServices.Enrichment fakeEnrichment;
    @Autowired
    private FakeDownstreamServices.Rules fakeRules;
    @Autowired
    private FakeDownstreamServices.Security fakeSecurity;
    @Autowired
    private FakeDownstreamServices.Limit fakeLimit;
    @Autowired
    private FakeDownstreamServices.Ledger fakeLedger;
    @Autowired
    private FakeDownstreamServices.AntiFraud fakeAntiFraud;

    @Autowired
    private SqsClient sqsClient;
    @Value("${aws.sqs.compensation-transaction-queue-name}")
    private String compensationQueueName;

    @BeforeEach
    void resetFakes() {
        fakeEnrichment.reset();
        fakeRules.reset();
        fakeSecurity.reset();
        fakeLimit.reset();
        fakeLedger.reset();
        fakeAntiFraud.reset();
        drainCompensationQueue();
    }

    private String compensationQueueUrl() {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(compensationQueueName).build()).queueUrl();
    }

    private void drainCompensationQueue() {
        for (Message message : receiveCompensationMessages()) {
            sqsClient.deleteMessage(DeleteMessageRequest.builder()
                    .queueUrl(compensationQueueUrl())
                    .receiptHandle(message.receiptHandle())
                    .build());
        }
    }

    private List<Message> receiveCompensationMessages() {
        return sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(compensationQueueUrl())
                .maxNumberOfMessages(10)
                .waitTimeSeconds(1)
                .build()).messages();
    }

    @Test
    @DisplayName("approves when every dependency approves")
    void approves_when_every_dependency_approves() throws Exception {
        var request = AuthorizeTransactionRequestFixture.domesticPurchaseWithChip().build();

        var response = authorizerTestClient.authorizeTransaction(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_APPROVED);
        assertThat(receiveCompensationMessages()).isEmpty();
    }

    @Test
    @DisplayName("declines in phase 1 (simulation) without calling phase 2 (commit)")
    void declines_in_phase_1_without_calling_phase_2() throws Exception {
        fakeSecurity.deny(ReasonCode.REASON_CODE_INVALID_PIN, "Invalid PIN");
        var request = AuthorizeTransactionRequestFixture.domesticPurchaseWithChip().build();

        var response = authorizerTestClient.authorizeTransaction(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_DECLINED);
        assertThat(fakeAntiFraud.callCount.get()).isZero();
        assertThat(fakeLedger.callCount.get()).isEqualTo(1); // only the phase 1 SIMULATION call
        assertThat(receiveCompensationMessages()).isEmpty();
    }

    @Test
    @DisplayName("triggers compensation (saga) when phase 2 partially fails")
    void triggers_compensation_when_phase_2_partially_fails() throws Exception {
        fakeLedger.denyCommit(ReasonCode.REASON_CODE_INSUFFICIENT_FUNDS, "Simulated decline on the ledger commit");
        var request = AuthorizeTransactionRequestFixture.domesticPurchaseWithChip().build();

        var response = authorizerTestClient.authorizeTransaction(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_DECLINED);
        var compensationMessages = receiveCompensationMessages();
        assertThat(compensationMessages).hasSize(1);
        assertThat(compensationMessages.get(0).body()).isEqualTo(response.getTransactionId());
    }

    @Test
    @DisplayName("honors the timeout configured per dependency")
    void honors_the_timeout_configured_per_dependency() throws Exception {
        fakeRules.slow(3000); // longer than the 2000ms timeout configured for this test
        var request = AuthorizeTransactionRequestFixture.domesticPurchaseWithChip().build();

        var response = authorizerTestClient.authorizeTransaction(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_DECLINED);
    }
}
