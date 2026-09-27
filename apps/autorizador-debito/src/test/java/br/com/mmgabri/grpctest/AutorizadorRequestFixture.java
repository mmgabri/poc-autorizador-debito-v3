package br.com.mmgabri.grpctest;

import br.com.mmgabri.grpc.autorizador.v1.AutorizadorRequest;
import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;

import java.time.Instant;
import java.util.UUID;

/**
 * Builder de requests válidos pra AutorizadorService, usado pelos testes de
 * integração. Cada fábrica estática representa um produto do catálogo
 * (products_config.yml) já com os campos ISO mínimos preenchidos; os testes
 * só sobrescrevem o que for relevante pro cenário.
 */
public final class AutorizadorRequestFixture {

    private final AutorizadorRequest.Builder builder;

    private AutorizadorRequestFixture() {
        builder = AutorizadorRequest.newBuilder()
                .setHeaderMessageGrpc(HeaderMessageGrpc.newBuilder()
                        .setCorrelationId(UUID.randomUUID().toString())
                        .setBandeira("MASTER")
                        .setPlataforma("TESTE")
                        .setTimestamp(Instant.now().toString())
                        .setMessage("teste de integração")
                        .build())
                .putMessageIso("mti", "0200") // -> produto COMPRA_NACIONAL_COM_CHIP_SENHA_MASTER
                .putMessageIso("002", "5555666677778888") // número do cartão
                .putMessageIso("004", "000000010000")     // valor da transação
                .putMessageIso("052", "1234")             // senha
                .putMessageIso("055", "CHIPDATA");        // dados do chip

    }

    /** Compra nacional com chip + senha, produto padrão usado na maioria dos cenários. */
    public static AutorizadorRequestFixture compraNacionalComChip() {
        return new AutorizadorRequestFixture();
    }

    public AutorizadorRequestFixture comCorrelationId(String correlationId) {
        builder.setHeaderMessageGrpc(builder.getHeaderMessageGrpc().toBuilder().setCorrelationId(correlationId).build());
        return this;
    }

    public AutorizadorRequestFixture comValor(String valor) {
        builder.putMessageIso("004", valor);
        return this;
    }

    public AutorizadorRequest build() {
        return builder.build();
    }
}
