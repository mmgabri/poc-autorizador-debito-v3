package br.com.mmgabri.grpctest;

import br.com.mmgabri.adapters.grpc.server.AutorizadorGrpcServer;
import br.com.mmgabri.grpc.antifraud.v1.AntiFraudServiceGrpc;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorServiceGrpc;
import br.com.mmgabri.grpc.enrichment.v1.DataEnrichmentServiceGrpc;
import br.com.mmgabri.grpc.ledger.v1.LedgerServiceGrpc;
import br.com.mmgabri.grpc.limit.v1.LimiteServiceGrpc;
import br.com.mmgabri.grpc.rules.v1.RulesServiceGrpc;
import br.com.mmgabri.grpc.security.v1.SegurancaServiceGrpc;
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
 * Substitui os 6 canais gRPC de saída do autorizador-debito (enrichment, rules,
 * security, limit, ledger, antifraud) por um servidor in-process controlado pelo
 * teste - ver {@link FakeDownstreamServices}. O restante do contexto Spring
 * (orquestração real em UseCaseAuthorization/ProcessTransaction) não é tocado.
 *
 * Também expõe um client in-process para o próprio AutorizadorGrpcServer, pra o
 * teste disparar a requisição de entrada como um chamador real faria (gRPC de
 * verdade nas duas pontas, sem TCP).
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
    public DataEnrichmentServiceGrpc.DataEnrichmentServiceBlockingV2Stub testEnrichmentStub(ManagedChannel inProcessDownstreamChannel) {
        return DataEnrichmentServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public RulesServiceGrpc.RulesServiceBlockingV2Stub testRulesStub(ManagedChannel inProcessDownstreamChannel) {
        return RulesServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public SegurancaServiceGrpc.SegurancaServiceBlockingV2Stub testSecurityStub(ManagedChannel inProcessDownstreamChannel) {
        return SegurancaServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public LimiteServiceGrpc.LimiteServiceBlockingV2Stub testLimitStub(ManagedChannel inProcessDownstreamChannel) {
        return LimiteServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public LedgerServiceGrpc.LedgerServiceBlockingV2Stub testLedgerStub(ManagedChannel inProcessDownstreamChannel) {
        return LedgerServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean
    @Primary
    public AntiFraudServiceGrpc.AntiFraudServiceBlockingV2Stub testAntiFraudStub(ManagedChannel inProcessDownstreamChannel) {
        return AntiFraudServiceGrpc.newBlockingV2Stub(inProcessDownstreamChannel);
    }

    @Bean(destroyMethod = "shutdownNow")
    public Server inProcessInboundServer(AutorizadorGrpcServer autorizadorGrpcServer) throws IOException {
        return InProcessServerBuilder.forName(inboundServerName)
                .directExecutor()
                .addService(autorizadorGrpcServer)
                .build()
                .start();
    }

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel inProcessInboundChannel(Server inProcessInboundServer) {
        return InProcessChannelBuilder.forName(inboundServerName).directExecutor().build();
    }

    @Bean
    public AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub autorizadorTestClient(ManagedChannel inProcessInboundChannel) {
        return AutorizadorServiceGrpc.newBlockingV2Stub(inProcessInboundChannel);
    }
}
