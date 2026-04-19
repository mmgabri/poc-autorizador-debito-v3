package br.com.mmgabri.services;

import br.com.mmgabri.grpc.SegurancaRequest;
import br.com.mmgabri.grpc.SegurancaResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class SegurancaService {

    @Value("${custom.sleep:100}")
    long customSleepMillis;

    @SneakyThrows
    public SegurancaResponse execute(SegurancaRequest request) {

        if (request.getSleepSeguranca() > 0) {
            sleep(Duration.ofMillis(request.getSleepSeguranca()));
        } else {
            sleep(Duration.ofMillis(customSleepMillis));
        }

        if ("999".equals(request.getCustomReturnSeguranca())) {
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

        SegurancaResponse response = SegurancaResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setApproved("000".equals(request.getCustomReturnSeguranca()))
                .setErrorCode(request.getCustomReturnSeguranca())
                .setErrorDescription(getMessage(request.getCustomReturnSeguranca()))
                .setContaId(request.getContaId())
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
