package br.com.mmgabri.grpctest;

import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesRequest;
import br.com.itau.debit.authorizer.rulesengine.v1.CheckRulesResponse;
import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.mmgabri.adapters.grpc.config.CorrelationMetadata;
import br.com.mmgabri.adapters.grpc.server.CorrelationServerInterceptor;
import br.com.mmgabri.application.domains.HeaderMessage;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The correlation context travels as gRPC metadata: the client attaches it
 * (CorrelationMetadata) and the server exposes it in the MDC only during the call
 * (CorrelationServerInterceptor).
 */
class CorrelationPropagationTest {

    private final AtomicReference<Map<String, String>> mdcInsideHandler = new AtomicReference<>();
    private final AtomicReference<Map<String, String>> mdcAfterHandler = new AtomicReference<>();

    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        var name = InProcessServerBuilder.generateName();
        var service = new RulesEngineServiceGrpc.RulesEngineServiceImplBase() {
            @Override
            public void checkRules(CheckRulesRequest request, StreamObserver<CheckRulesResponse> responseObserver) {
                mdcInsideHandler.set(MDC.getCopyOfContextMap());
                var result = BusinessResult.newBuilder().setApproved(true).build();
                responseObserver.onNext(CheckRulesResponse.newBuilder().setResult(result).build());
                responseObserver.onCompleted();
            }
        };
        var intercepted = ServerInterceptors.intercept(service, new CorrelationServerInterceptor());
        // directExecutor: handler and interceptor run on the same thread, so we can
        // check that the MDC was cleared after the call.
        server = InProcessServerBuilder.forName(name).directExecutor().addService(intercepted).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
        MDC.clear();
    }

    @Test
    void propagates_correlationId_and_transactionId_from_client_to_server_mdc() {
        var header = HeaderMessage.builder().correlationId("corr-123").transactionId("tx-456").build();
        var stub = CorrelationMetadata.attach(RulesEngineServiceGrpc.newBlockingV2Stub(channel), header);

        callAndCaptureMdcAfter(stub);

        assertThat(mdcInsideHandler.get())
                .containsEntry("correlationId", "corr-123")
                .containsEntry("transactionId", "tx-456");
        assertThat(mdcAfterHandler.get()).isNullOrEmpty();
    }

    @Test
    void omits_missing_keys_without_breaking_the_call() {
        var header = HeaderMessage.builder().correlationId("corr-only").build();
        var stub = CorrelationMetadata.attach(RulesEngineServiceGrpc.newBlockingV2Stub(channel), header);

        callAndCaptureMdcAfter(stub);

        assertThat(mdcInsideHandler.get())
                .containsEntry("correlationId", "corr-only")
                .doesNotContainKey("transactionId");
    }

    private void callAndCaptureMdcAfter(RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub stub) {
        try {
            stub.checkRules(CheckRulesRequest.getDefaultInstance());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        mdcAfterHandler.set(MDC.getCopyOfContextMap());
    }
}
