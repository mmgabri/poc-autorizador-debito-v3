package br.com.mmgabri.domains;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class FormatadorRequest {
    private Map<String, String> messageIso;
    private String customReturnDataEnrichment;
    private String customReturnSeguranca;
    private String customReturnLimitePortador;
    private String customReturnLimite;
    private String customReturnLancamentoConta;
    private String customReturnFraude;
    private int sleepSeguranca;
    private int sleepDataEnrichment;
    private int sleepLimitePortador;
    private int sleepLimite;
    private int sleepLancamentoConta;
    private int sleepFraude;
}

