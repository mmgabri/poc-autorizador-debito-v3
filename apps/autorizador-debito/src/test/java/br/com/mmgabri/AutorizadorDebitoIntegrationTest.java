package br.com.mmgabri;

import br.com.mmgabri.adapters.dynamodb.repository.IdempotencyRepository;
import br.com.mmgabri.adapters.dynamodb.repository.ServiceContextRepository;
import br.com.mmgabri.adapters.dynamodb.repository.TransactionContextRepository;
import br.com.mmgabri.application.services.CompensationTransactionService;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorServiceGrpc;
import br.com.mmgabri.grpctest.AutorizadorRequestFixture;
import br.com.mmgabri.grpctest.FakeDownstreamServices;
import br.com.mmgabri.grpctest.GrpcInProcessTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Teste de integração ponta a ponta do autorizador-debito: entra por gRPC real
 * (in-process) em AutorizadorGrpcServer, atravessa toda a orquestração real
 * (ProcessTransaction / UseCaseAuthorization, incluindo as chamadas paralelas de
 * Fase 1/Fase 2), e sai por gRPC real (in-process) pras 6 dependências, cujo
 * comportamento é controlado pelos fakes em {@link FakeDownstreamServices}.
 * DynamoDB (idempotência/contexto) e a notificação SQS de compensação são
 * mockados - infraestrutura de estado, não é o que este teste valida.
 */
@SpringBootTest(
        classes = Application.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.grpc.server.port=0",
                "management.datadog.metrics.export.enabled=false",
                // Timeouts reais de dependência (application.yml) são de 1h - bom pra produção,
                // inviável pra testar timeout num teste. Encurtados só aqui.
                "grpc.enrichment-service-client.timeout=2000",
                "grpc.rules-service-client.timeout=2000",
                "grpc.security-service-client.timeout=2000",
                "grpc.limit-service-client.timeout=2000",
                "grpc.ledger-service-client.timeout=2000",
                "grpc.antifraud-service-client.timeout=2000"
        })
@Import(GrpcInProcessTestConfig.class)
class AutorizadorDebitoIntegrationTest {

    private static final String STATUS_APROVADO = "00";
    private static final String STATUS_NEGADO = "96";

    @Autowired
    private AutorizadorServiceGrpc.AutorizadorServiceBlockingV2Stub autorizadorTestClient;

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

    @MockitoBean
    private IdempotencyRepository idempotencyRepository;
    @MockitoBean
    private TransactionContextRepository transactionContextRepository;
    @MockitoBean
    private ServiceContextRepository serviceContextRepository;
    @MockitoBean
    private CompensationTransactionService compensationTransactionService;

    @BeforeEach
    void resetFakes() {
        fakeEnrichment.reset();
        fakeRules.reset();
        fakeSecurity.reset();
        fakeLimit.reset();
        fakeLedger.reset();
        fakeAntiFraud.reset();
    }

    @Test
    @DisplayName("aprova quando todas as dependências aprovam")
    void aprova_quando_todas_as_dependencias_aprovam() throws Exception {
        var request = AutorizadorRequestFixture.compraNacionalComChip().build();

        var response = autorizadorTestClient.autorizarTransacao(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_APROVADO);
        verifyNoInteractions(compensationTransactionService);
    }

    @Test
    @DisplayName("nega na fase 1 (simulação) sem chamar a fase 2 (efetivação)")
    void nega_na_fase_1_sem_chamar_fase_2() throws Exception {
        fakeSecurity.deny("SEN", "Senha inválida");
        var request = AutorizadorRequestFixture.compraNacionalComChip().build();

        var response = autorizadorTestClient.autorizarTransacao(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_NEGADO);
        assertThat(fakeAntiFraud.callCount.get()).isZero();
        assertThat(fakeLedger.callCount.get()).isEqualTo(1); // só a chamada de SIMULACAO da fase 1
        verifyNoInteractions(compensationTransactionService);
    }

    @Test
    @DisplayName("aciona a compensação (saga) quando a fase 2 falha parcialmente")
    void aciona_compensacao_quando_fase_2_falha_parcialmente() throws Exception {
        fakeLedger.denyEfetivacao("999", "Falha simulada na efetivação do ledger");
        var request = AutorizadorRequestFixture.compraNacionalComChip().build();

        var response = autorizadorTestClient.autorizarTransacao(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_NEGADO);
        verify(compensationTransactionService).publish(anyString());
    }

    @Test
    @DisplayName("respeita o timeout configurado por dependência")
    void respeita_o_timeout_configurado_por_dependencia() throws Exception {
        fakeRules.slow(3000); // maior que o timeout de 2000ms configurado pra este teste
        var request = AutorizadorRequestFixture.compraNacionalComChip().build();

        var response = autorizadorTestClient.autorizarTransacao(request);

        assertThat(response.getMessageIsoMap().get("039")).isEqualTo(STATUS_NEGADO);
    }
}
