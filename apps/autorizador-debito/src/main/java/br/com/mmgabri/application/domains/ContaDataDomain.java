package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContaDataDomain {
    private String contaId;
    private String categoria;
    private String segmento;
    private String tipo;
}
