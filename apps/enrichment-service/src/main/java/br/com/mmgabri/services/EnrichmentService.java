package br.com.mmgabri.services;

import br.com.mmgabri.grpc.enrichment.v1.*;

import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class EnrichmentService {

    @Value("${custom.sleep:100}")
    long customSleepMillis;

    @SneakyThrows
    public EnrichByCardResponse execute(EnrichByCardRequest request) {

        if (request.getSleepDataEnrichment() > 0) {
            sleep(Duration.ofMillis(request.getSleepDataEnrichment()));
        } else {
            sleep(Duration.ofMillis(1));
        }

        if ("999".equals(request.getCustomReturnDataEnrichment())) {
            throw new RuntimeException("Erro comandado pelo chamador");
        }

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                .setCorrelationId(request.getHeaderMessageGrpc().getCorrelationId())
                .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                .setMessage(request.getHeaderMessageGrpc().getMessage())
                .build();

        CartaoData cartao = CartaoData.newBuilder()
                .setNumeroCartao(request.getNumeroCartao())
                .setHashCartao("d9b06a17-8c45-4f1a-9b6f-f0b06a6d6b1c")
                .setContaId("6f1c3f2e-1f4a-4b0e-9f7a-2c8e0a7d6c31")
                .setTitular("1")
                .build();

        ContaData conta = ContaData.newBuilder()
                .setContaId("8a3d5c12-7b9e-4f6a-b2c1-3e9f0a4d7c58")
                .setCategoria("698")
                .setSegmento("L012")
                .setTipo("C")
                .build();

        ClienteData cliente = ClienteData.newBuilder()
                .setClienteId("2d7f4a9c-5c3b-4e1f-8a6d-9b0c1e2f3a47")
                .setCpfCnpj("2776914540")
                .setTelefone("11995963448")
                .build();

        TokenData token = TokenData.newBuilder()
                .setTokenId("c4a1b8d2-3f6e-4a9c-8d1b-5e7f0a2c9b34")
                .setStatus("OK")
                .setCarteira("963")
                .build();

        EnrichByCardResponse response = EnrichByCardResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setApproved("000".equals(request.getCustomReturnDataEnrichment()))
                .setErrorCode(request.getCustomReturnDataEnrichment())
                .setErrorDescription(getMessage(request.getCustomReturnDataEnrichment()))
                .setCartao(cartao)
                .setCliente(cliente)
                .setConta(conta)
                .setToken(token)
                .build();

        return response;

    }

    private String getMessage(String errorCode) {

        switch (errorCode) {
            case "SDO":
                return "Saldo Insuficiente";
            case "LIM":
                return "Limite Insuficiente";
            case "SEN":
                return "Senha invalida";
            case "CHP":
                return "Erro na autenticação do chip";
            case "CVV":
                return "CVV Invalido";
            case "CNE":
                return "Cartão invalido";
            case "IND":
                return "Sistema indisponivel";
            case "TIM":
                return "Timeout  na aplicação";
            case "EIN":
                return "Error de sistema";
            case "ERR":
                return "Erro de sistema";
            default:
                return "Processamento efetuado";
        }
    }

    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
