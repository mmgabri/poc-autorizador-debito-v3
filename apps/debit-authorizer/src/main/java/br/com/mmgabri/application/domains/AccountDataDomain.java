package br.com.mmgabri.application.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDataDomain {
    private String accountId;
    private String category;
    private String segment;
    private String type;
}
