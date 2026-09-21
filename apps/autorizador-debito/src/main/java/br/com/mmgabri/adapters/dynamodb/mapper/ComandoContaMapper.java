package br.com.mmgabri.adapters.dynamodb.mapper;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.adapters.grpc.client.SecurityServiceGrpcClient;
import br.com.mmgabri.application.domains.ExecutionSimulationConfig;
import br.com.mmgabri.application.domains.HeaderMessage;
import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.application.services.MetricsService;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.domain.LedgerEfetivacaoResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Constrói os objetos ligados à efetivação assíncrona do ledger (comando_conta):
 * o registro inicial no DynamoDB, a mensagem publicada na fila SQS, o esqueleto
 * usado nas transições de status, e o resultado final devolvido ao caso de uso.
 */
@Service
@RequiredArgsConstructor
public class ComandoContaMapper {

    public ComandoContaEntity toPendingEntity(Payload payload, String contaId) {
        return ComandoContaEntity.builder()
                .correlationId(payload.getHeaderMessage().getCorrelationId())
                .contaId(contaId)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    public ComandoContaRequest toComandoContaRequest(Payload payload, String contaId) {
        HeaderMessage header = payload.getHeaderMessage();
        ExecutionSimulationConfig execConfig = payload.getExecutionSimulationConfig();

        return ComandoContaRequest.builder()
                .correlationId(header.getCorrelationId())
                .transactionId(header.getTransactionId())
                .bandeira(header.getBandeira())
                .plataforma(header.getPlataforma())
                .timestamp(header.getTimestamp())
                .message(header.getMessage())
                .contaId(contaId)
                .customReturnLedger(execConfig.getCustomReturnLedger())
                .sleepLedgerEfetivacao(execConfig.getSleepLedgerEfetivacao())
                .build();
    }

    public ComandoContaEntity toTransitionFields(String correlationId) {
        return ComandoContaEntity.builder()
                .correlationId(correlationId)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    public LedgerEfetivacaoResponse toLedgerEfetivacaoResponse(ComandoContaEntity entity) {
        return LedgerEfetivacaoResponse.builder()
                .correlationId(entity.getCorrelationId())
                .contaId(entity.getContaId())
                .approved(Boolean.TRUE.equals(entity.getApproved()))
                .errorCode(entity.getErrorCode())
                .errorDescription(entity.getErrorDescription())
                .build();
    }
}