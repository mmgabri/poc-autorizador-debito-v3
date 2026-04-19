package br.com.mmgabri.application.mappers;

import br.com.mmgabri.application.domains.*;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.EnrichByCardResponse;
import org.springframework.stereotype.Service;

@Service
public class PayloadMapper {

    public Payload map(AutorizadorRequest request, ProductDomain product, String transactionId) {

        HeaderMessage header = HeaderMessage.builder()
                .transactionId(transactionId)
                .correlationId(request.getHeaderMessageGrpc().getCorrelationId())
                .bandeira(request.getHeaderMessageGrpc().getBandeira())
                .plataforma(request.getHeaderMessageGrpc().getPlataforma())
                .timestamp(request.getHeaderMessageGrpc().getTimestamp())
                .message(request.getHeaderMessageGrpc().getMessage())
                .build();

        ExecutionSimulationConfig executionSimulationConfig = ExecutionSimulationConfig.builder()
                .customReturnDataEnrichment(request.getCustomReturnDataEnrichment())
                .sleepDataEnrichment(request.getSleepDataEnrichment())
                .customReturnRules(request.getCustomReturnRules())
                .sleepRules(request.getSleepRules())
                .customReturnSeguranca(request.getCustomReturnSeguranca())
                .sleepSeguranca(request.getSleepSeguranca())
                .customReturnLimit(request.getCustomReturnLimit())
                .sleepLimitEfetivacao(request.getSleepLimitEfetivacao())
                .sleepLimitSimulacao(request.getSleepLimitSimulacao())
                .customReturnLedger(request.getCustomReturnLedger())
                .sleepLedgerEfetivacao(request.getSleepLedgerEfetivacao())
                .sleepLedgerSimulacao(request.getSleepLedgerSimulacao())
                .customReturnFraude(request.getCustomReturnFraude())
                .sleepFraude(request.getSleepFraude())
                .transactionIdReversal(request.getTransactionIdReversal())
                .build();

        return Payload.builder()
                .executionSimulationConfig(executionSimulationConfig)
                .headerMessage(header)
                .productDomain(product)
                .messageIso(request.getMessageIsoMap())
                .build();
    }

    public Payload mapEnrichedData(Payload payload, EnrichByCardResponse enrichByCardResponse) {

        CartaoDataDomain cartao = CartaoDataDomain.builder()
                .numeroCartao(enrichByCardResponse.getCartao().getNumeroCartao())
                .hashCartao(enrichByCardResponse.getCartao().getHashCartao())
                .titular(enrichByCardResponse.getCartao().getTitular())
                .contaId(enrichByCardResponse.getCartao().getContaId())
                .build();

        ContaDataDomain conta = ContaDataDomain.builder()
                .contaId(enrichByCardResponse.getConta().getContaId())
                .categoria(enrichByCardResponse.getConta().getCategoria())
                .segmento(enrichByCardResponse.getConta().getSegmento())
                .tipo(enrichByCardResponse.getConta().getTipo())
                .build();

        ClienteDataDomain cliente = ClienteDataDomain.builder()
                .clienteId(enrichByCardResponse.getCliente().getClienteId())
                .cpfCnpj(enrichByCardResponse.getCliente().getCpfCnpj())
                .telefone(enrichByCardResponse.getCliente().getTelefone())
                .build();

        TokenDataDomain token = TokenDataDomain.builder()
                .tokenId(enrichByCardResponse.getToken().getTokenId())
                .status(enrichByCardResponse.getToken().getStatus())
                .carteira(enrichByCardResponse.getToken().getCarteira())
                .build();

        DataEnrichmentDomain dadosEnriquecidos = DataEnrichmentDomain.builder()
                .cartao(cartao)
                .conta(conta)
                .cliente(cliente)
                .token(token)
                .build();

        payload.setDataEnrichment(dadosEnriquecidos);

        return payload;

    }
}