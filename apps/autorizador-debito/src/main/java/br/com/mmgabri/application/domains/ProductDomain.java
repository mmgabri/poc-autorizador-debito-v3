package br.com.mmgabri.application.domains;

import br.com.mmgabri.application.domains.enuns.TransactionOperationEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDomain {
    private String productName;
    private TransactionOperationEnum transactionOperation;
    private boolean enabled;
    private String roteiroContabil;
    private String codigoLiteral;
    private List<String> configSeguranca = new ArrayList<>();
}
