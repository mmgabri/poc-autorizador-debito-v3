package br.com.mmgabri.domains;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class HeaderMessage {
    private String transactionId;
    private String correlationId;
    private String bandeira;
    private String plataforma;
    private String timestamp;
    private String message;
    private boolean isReversal;
}

