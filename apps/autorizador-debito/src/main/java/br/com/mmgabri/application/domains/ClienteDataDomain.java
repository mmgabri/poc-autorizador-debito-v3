package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClienteDataDomain {
    private String clienteId;
    private String cpfCnpj;
    private String telefone;
}