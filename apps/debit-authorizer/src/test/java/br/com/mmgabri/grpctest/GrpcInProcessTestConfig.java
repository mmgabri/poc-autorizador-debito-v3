package br.com.mmgabri.grpctest;

import br.com.mmgabri.adapters.grpc.server.AuthorizationGrpcServer;
import br.com.itau.debit.authorizer.antifraud.v1.AntifraudServiceGrpc;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizationServiceGrpc;
import br.com.itau.debit.authorizer.enrichment.v1.EnrichmentServiceGrpc;
import br.com.itau.debit.authorizer.accountposting.v1.AccountPostingServiceGrpc;
import br.com.itau.debit.authorizer.limit.v1.LimitServiceGrpc;
import br.com.itau.debit.authorizer.rulesengine.v1.RulesEngineServiceGrpc;
import br.com.itau.debit.authorizer.security.v1.SecurityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.util.UUID;

/**
 * Replaces the 6 outgoing gRPC channels of debit-authorizer (enrichment, rules,
 * security, limit, ledger, antifraud) with an in-process server controlled by the
 * test - see {@link FakeDownstreamServices}. The rest of the Spring context (real
 * orchestration in UseCaseAuthorization/ProcessTransaction) is untouched.
 *
 * Also exposes an in-process client for AuthorizationGrpcServer itself, so the test
 * fires the inbound request as a real caller would (real gRPC on both ends, no TCP).
 */
@TestConfiguration
public class GrpcInProcessTestConfig {

    private final String downstreamServerName = "downstream-test-" + UUID.randomUUID();
    private final String inboundServerName = "inbound-test-" + UUID.randomUUID();

    @Bean
    public FakeDownstreamServices.Enrichment fakeEnrichment() {
        return new FakeDownstreamServices.Enrichment();
    }

    @Bean
    public FakeDownstreamServices.Rules fakeRules() {
        return new FakeDownstreamServices.Rules();
    }

    @Bean
    public FakeDownstreamServices.Security fakeSecurity() {
        return new FakeDownstreamServices.Security();
    }

    @Bean
    public FakeDownstreamServices.Limit fakeLimit() {
        return new FakeDownstreamServices.Limit();
    }

    @Bean
    public FakeDownstreamServices.Ledger fakeLedger() {
        return new FakeDownstreamServices.Ledger();
    }

    @Bean
    public FakeDownstreamServices.AntiFraud fakeAntiFraud() {
        return new FakeDownstreamServices.AntiFraud();
    }

    @Bean(destroyMethod = "shutdownNow")
    public Server inProcessDownstreamServer(FakeDownstreamServices.Enrichment enrichment,
                                             FakeDownstreamServices.Rules rules,
                                             FakeDownstreamServices.Security security,
                                             FakeDownstreamServices.Limit limit,
                                             FakeDownstreamServices.Ledger ledger,
                                             FakeDownstreamServices.AntiFraud antiFraud) throws IOException {
        return InProcessServerBuilder.forName(downstreamServerName)
                .directExecutor()
                .addService(enrichment)
                .addService(rules)
                .addService(security)
                .addService(limit)
                .addService(ledger)
                .addService(antiFraud)
                .build()
                .start();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel inProcessDownstreamChannel(Server inProcessDownstreamServer) {
        return InProcessChannelBuilder.forName(downstreamServerName).directExecutor().build();
    }

    @Bean
    @Primary
    public EnrichmentServiceGrpc.EnrichmentServiceBlockingV2Stub testEnrichmentStub(ManagedChannel inProcessDownstreamChannel) {
        return EnrichmentServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public RulesEngineServiceGrpc.RulesEngineServiceBlockingV2Stub testRulesStub(ManagedChannel inProcessDownstreamChannel) {
        return RulesEngineServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public SecurityServiceGrpc.SecurityServiceBlockingV2Stub testSecurityStub(ManagedChannel inProcessDownstreamChannel) {
        return SecurityServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public LimitServiceGrpc.LimitServiceBlockingV2Stub testLimitStub(ManagedChannel inProcessDownstreamChannel) {
        return LimitServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public AccountPostingServiceGrpc.AccountPostingServiceBlockingV2Stub testLedgerStub(ManagedChannel inProcessDownstreamChannel) {
        return AccountPostingServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public AntifraudServiceGrpc.AntifraudServiceBlockingV2Stub testAntiFraudStub(ManagedChannel inProcessDownstreamChannel) {
        return AntifraudServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean(destroyMethod = "shutdownNow")
    public Server inProcessInboundServer(AuthorizationGrpcServer authorizationGrpcServer) throws IOException {
        return InProcessServerBuilder.forName(inboundServerName)
                .directExecutor()
                .addService(authorizationGrpcServer)
                .build()
                .start();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel inProcessInboundChannel(Server inProcessInboundServer) {
        return InProcessChannelBuilder.forName(inboundServerName).directExecutor().build();
    }

    @Bean
    public AuthorizationServiceGrpc.AuthorizationServiceBlockingV2Stub authorizerTestClient(ManagedChannel inProcessInboundChannel) {
        return AuthorizationServiceGrpc.newBlockingV2Stub(inProcessInboundChannel);
    }
}
