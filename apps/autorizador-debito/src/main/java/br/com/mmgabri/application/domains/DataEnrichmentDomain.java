package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DataEnrichmentDomain {
    private CartaoDataDomain cartao;
    private ContaDataDomain conta;
    private ClienteDataDomain cliente;
    private TokenDataDomain token;
}

