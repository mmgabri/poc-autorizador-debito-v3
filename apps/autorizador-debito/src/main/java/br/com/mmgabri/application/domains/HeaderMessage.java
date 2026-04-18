package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class HeaderMessage {
    private String transactionId;
    private String correlationId;
    private String bandeira;
    private String plataforma;
    private String timestamp;
    private String message;
}

