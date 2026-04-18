package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenDataDomain {
    private String tokenId;
    private String status;
    private String carteira;
}