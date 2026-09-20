package br.com.mmgabri.services;

import br.com.mmgabri.grpc.antifraud.v1.AntiFraudRequest;
import br.com.mmgabri.grpc.antifraud.v1.AntiFraudResponse;
import br.com.mmgabri.grpc.comuns.v1.HeaderMessageGrpc;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AntiFraudService {

    @SneakyThrows
    public AntiFraudResponse execute(AntiFraudRequest request) {
        if (request.getSleepFraude() > 0) {
            sleep(Duration.ofMillis(request.getSleepFraude()));
        } else {
            sleep(Duration.ofMillis(1));
        }

        if ("999".equals(request.getCustomReturnFraude())) {
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

        AntiFraudResponse response = AntiFraudResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .setApproved("000".equals(request.getCustomReturnFraude()))
                .setErrorCode(request.getCustomReturnFraude())
                .setErrorDescription(getMessage(request.getCustomReturnFraude()))
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

    @SneakyThrows
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
