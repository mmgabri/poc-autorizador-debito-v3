package br.com.mmgabri.application.domains;

import br.com.mmgabri.config.ProductConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payload {
    private HeaderMessage headerMessage;
    private Map<String, String> messageIso;
    private ProductDomain productDomain;
    private DataEnrichmentDomain dataEnrichment;
    private ExecutionSimulationConfig executionSimulationConfig;
    private TransactionExecutionContext transactionExecutionContextOrigin;
}

