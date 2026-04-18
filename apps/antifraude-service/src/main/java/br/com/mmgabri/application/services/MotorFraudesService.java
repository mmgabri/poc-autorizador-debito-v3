package br.com.mmgabri.application.services;

import br.com.mmgabri.application.domains.FraudesRequest;
import br.com.mmgabri.application.domains.FraudesResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MotorFraudesService {
    private static final Logger logger = LoggerFactory.getLogger(MotorFraudesService.class);

    @SneakyThrows
    public FraudesResponse execute(FraudesRequest request) {
        if (request.getSleepFraude() > 0) {
            sleep(Duration.ofMillis(request.getSleepFraude()));
        } else {
            sleep(Duration.ofMillis(5));
        }

        if ("999".equals(request.getCustomReturnFraude())) {
            throw new RuntimeException("Erro comandado pelo chamador");
        }

        FraudesResponse response = FraudesResponse.builder()
                .aprovado("000".equals(request.getCustomReturnFraude()))
                .errorCode(request.getCustomReturnFraude())
                .errorDescription(getMessage(request.getCustomReturnFraude()))
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