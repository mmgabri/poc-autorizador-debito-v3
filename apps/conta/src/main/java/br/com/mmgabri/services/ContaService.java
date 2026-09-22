package br.com.mmgabri.services;

import br.com.mmgabri.adapters.grpc.client.LedgerServiceGrpcClient;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ContaService {

    private final LedgerServiceGrpcClient ledgerClient;

    public void execute(ComandoContaRequest payload) {

        sleep(Duration.ofMillis(payload.sleepLedgerEfetivacao()));

        RetornoContaRequest request = RetornoContaRequest.newBuilder()
                .setCorrelationId(payload.correlationId())
                .setInstanceId(payload.instanceId())
                .setErrorCode(payload.customReturnLedger())
                .setErrorDescription(getMessage(payload.customReturnLedger()))
                .setApproved("000".equals(payload.customReturnLedger()))
                .build();

        ledgerClient.execute(request);
    }

    // Simula processamento
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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
}
