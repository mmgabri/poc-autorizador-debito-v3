package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartaoDataDomain {
    private String numeroCartao;
    private String hashCartao;
    private String contaId;
    private String titular;
}
