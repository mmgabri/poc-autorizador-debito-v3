package br.com.mmgabri.application.domains;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
public class FraudesResponse {
    private boolean aprovado;
    private String errorCode;
    private String errorDescription;
}
