package br.com.mmgabri.services;

import br.com.mmgabri.grpc.LimiteRequest;
import br.com.mmgabri.grpc.LimiteResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class LimitService {

    @Value("${custom.sleep:100}")
    long customSleepMillis;

    @SneakyThrows
    public LimiteResponse execute(LimiteRequest request) {

        if (request.getSleepLimite() > 0) {
            sleep(Duration.ofMillis(request.getSleepLimite()));
        } else {
            sleep(Duration.ofMillis(customSleepMillis));
        }

        if ("999".equals(request.getCustomReturnLimite())) {
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

        LimiteResponse response = LimiteResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setApproved("000".equals(request.getCustomReturnLimite()))
                .setErrorCode(request.getCustomReturnLimite())
                .setErrorDescription(getMessage(request.getCustomReturnLimite()))
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
