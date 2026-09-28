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
    private CardDataDomain card;
    private AccountDataDomain account;
    private CustomerDataDomain customer;
    private TokenDataDomain token;
}
