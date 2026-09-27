package br.com.mmgabri.grpctest;

import br.com.mmgabri.grpc.antifraud.v1.AntiFraudRequest;
import br.com.mmgabri.grpc.antifraud.v1.AntiFraudResponse;
import br.com.mmgabri.grpc.antifraud.v1.AntiFraudServiceGrpc;
import br.com.mmgabri.grpc.enrichment.v1.CartaoData;
import br.com.mmgabri.grpc.enrichment.v1.ClienteData;
import br.com.mmgabri.grpc.enrichment.v1.ContaData;
import br.com.mmgabri.grpc.enrichment.v1.DataEnrichmentServiceGrpc;
import br.com.mmgabri.grpc.enrichment.v1.EnrichByCardRequest;
import br.com.mmgabri.grpc.enrichment.v1.EnrichByCardResponse;
import br.com.mmgabri.grpc.enrichment.v1.TokenData;
import br.com.mmgabri.grpc.ledger.v1.LedgerRequest;
import br.com.mmgabri.grpc.ledger.v1.LedgerResponse;
import br.com.mmgabri.grpc.ledger.v1.LedgerServiceGrpc;
import br.com.mmgabri.grpc.limit.v1.LimiteRequest;
import br.com.mmgabri.grpc.limit.v1.LimiteResponse;
import br.com.mmgabri.grpc.limit.v1.LimiteServiceGrpc;
import br.com.mmgabri.grpc.rules.v1.RulesRequest;
import br.com.mmgabri.grpc.rules.v1.RulesResponse;
import br.com.mmgabri.grpc.rules.v1.RulesServiceGrpc;
import br.com.mmgabri.grpc.security.v1.SegurancaRequest;
import br.com.mmgabri.grpc.security.v1.SegurancaResponse;
import br.com.mmgabri.grpc.security.v1.SegurancaServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Servidores gRPC fake (in-process) para as 6 dependências do autorizador-debito.
 * Cada um responde "aprovado" por padrão; os testes trocam o campo {@code responder}
 * pra simular negativa, lentidão (timeout) ou erro de uma dependência específica.
 * A chamada em si continua sendo gRPC real (protobuf, deadline, status code) - só a
 * rede é substituída por in-process.
 */
public class FakeDownstreamServices {

    private FakeDownstreamServices() {
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static class Enrichment extends DataEnrichmentServiceGrpc.DataEnrichmentServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<EnrichByCardRequest, EnrichByCardResponse> responder = Enrichment::approved;

        public void reset() {
            callCount.set(0);
            responder = Enrichment::approved;
        }

        private static EnrichByCardResponse approved(EnrichByCardRequest req) {
            return EnrichByCardResponse.newBuilder()
                    .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                    .setCartao(CartaoData.newBuilder().setNumeroCartao(req.getNumeroCartao()).setContaId("CONTA-TESTE").build())
                    .setConta(ContaData.newBuilder().setContaId("CONTA-TESTE").build())
                    .setCliente(ClienteData.newBuilder().setClienteId("CLIENTE-TESTE").build())
                    .setToken(TokenData.newBuilder().build())
                    .setApproved(true)
                    .build();
        }

        @Override
        public void enrichByCard(EnrichByCardRequest request, StreamObserver<EnrichByCardResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Rules extends RulesServiceGrpc.RulesServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<RulesRequest, RulesResponse> responder = Rules::approved;

        public void reset() {
            callCount.set(0);
            responder = Rules::approved;
        }

        private static RulesResponse approved(RulesRequest req) {
            return RulesResponse.newBuilder().setHeaderMessageGrpc(req.getHeaderMessageGrpc()).setApproved(true).build();
        }

        /** Demora {@code millis} antes de responder aprovado - simula dependência lenta/travada. */
        public void slow(long millis) {
            responder = req -> {
                sleep(millis);
                return approved(req);
            };
        }

        @Override
        public void validateRules(RulesRequest request, StreamObserver<RulesResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Security extends SegurancaServiceGrpc.SegurancaServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<SegurancaRequest, SegurancaResponse> responder = Security::approved;

        public void reset() {
            callCount.set(0);
            responder = Security::approved;
        }

        private static SegurancaResponse approved(SegurancaRequest req) {
            return SegurancaResponse.newBuilder()
                    .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                    .setContaId(req.getContaId())
                    .setApproved(true)
                    .build();
        }

        /** Passa a negar toda chamada com o código/descrição informados. */
        public void deny(String errorCode, String errorDescription) {
            responder = req -> SegurancaResponse.newBuilder()
                    .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                    .setContaId(req.getContaId())
                    .setApproved(false)
                    .setErrorCode(errorCode)
                    .setErrorDescription(errorDescription)
                    .build();
        }

        @Override
        public void validarSeguranca(SegurancaRequest request, StreamObserver<SegurancaResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Limit extends LimiteServiceGrpc.LimiteServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<LimiteRequest, LimiteResponse> responder = Limit::approved;

        public void reset() {
            callCount.set(0);
            responder = Limit::approved;
        }

        private static LimiteResponse approved(LimiteRequest req) {
            return LimiteResponse.newBuilder()
                    .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                    .setContaId(req.getContaId())
                    .setApproved(true)
                    .build();
        }

        @Override
        public void atualizarLimite(LimiteRequest request, StreamObserver<LimiteResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class Ledger extends LedgerServiceGrpc.LedgerServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<LedgerRequest, LedgerResponse> responder = Ledger::approved;

        public void reset() {
            callCount.set(0);
            responder = Ledger::approved;
        }

        private static LedgerResponse approved(LedgerRequest req) {
            return LedgerResponse.newBuilder()
                    .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                    .setContaId(req.getContaId())
                    .setApproved(true)
                    .build();
        }

        /**
         * Aprova a SIMULACAO (fase 1) normalmente, mas nega a EFETIVACAO (fase 2) -
         * gera exatamente o cenário de falha parcial na fase 2 que aciona a saga.
         */
        public void denyEfetivacao(String errorCode, String errorDescription) {
            responder = req -> {
                if (!"EFETIVACAO".equals(req.getTipoOperacao())) {
                    return approved(req);
                }
                return LedgerResponse.newBuilder()
                        .setHeaderMessageGrpc(req.getHeaderMessageGrpc())
                        .setContaId(req.getContaId())
                        .setApproved(false)
                        .setErrorCode(errorCode)
                        .setErrorDescription(errorDescription)
                        .build();
            };
        }

        @Override
        public void gerarLancamento(LedgerRequest request, StreamObserver<LedgerResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }

    public static class AntiFraud extends AntiFraudServiceGrpc.AntiFraudServiceImplBase {
        public final AtomicInteger callCount = new AtomicInteger();
        public volatile Function<AntiFraudRequest, AntiFraudResponse> responder = AntiFraud::approved;

        public void reset() {
            callCount.set(0);
            responder = AntiFraud::approved;
        }

        private static AntiFraudResponse approved(AntiFraudRequest req) {
            return AntiFraudResponse.newBuilder().setHeaderMessageGrpc(req.getHeaderMessageGrpc()).setApproved(true).build();
        }

        @Override
        public void validarFraude(AntiFraudRequest request, StreamObserver<AntiFraudResponse> responseObserver) {
            callCount.incrementAndGet();
            responseObserver.onNext(responder.apply(request));
            responseObserver.onCompleted();
        }
    }
}
