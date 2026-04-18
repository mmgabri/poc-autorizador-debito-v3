package br.com.mmgabri.domains;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class ReversalRequest {
    private Map<String, String> messageIso;
    private String transactionId;
    private String contaId;
    private String valor;
}
