package br.com.mmgabri.application.domains;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class FraudesRequest {
    private String transactionId;
    private String contaId;
    private String valor;
    private String tipoPessoa;
    private String productName;
    private String customReturnFraude;
    private int sleepFraude;
}
