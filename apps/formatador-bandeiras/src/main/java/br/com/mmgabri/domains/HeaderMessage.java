package br.com.mmgabri.domains;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class HeaderMessage {
    private String transactionId;
    private String correlationId;
    private String bandeira;
    private String plataforma;
    private String timestamp;
    private String message;
    private boolean isReversal;
}

