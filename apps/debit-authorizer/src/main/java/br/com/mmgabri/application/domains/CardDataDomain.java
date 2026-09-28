package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardDataDomain {
    private String cardNumber;
    private String cardHash;
    private String accountId;
    private String holderName;
}
