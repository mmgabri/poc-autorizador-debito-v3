package br.com.mmgabri.adapters.dynamodb.mapper;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.domain.ComandoContaRequest;
import br.com.mmgabri.grpc.ledger.v1.LedgerRequest;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ComandoContaMapper {

    /**
     * Registro inicial (PENDING), gravado antes de publicar no SQS para o conta.
     */
    public ComandoContaEntity toPendingEntity(LedgerRequest request, String instanceId) {
        return ComandoContaEntity.builder()
                .correlationId(request.getHeaderMessageGrpc().getCorrelationId())
                .contaId(request.getContaId())
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    /**
     * Mensagem publicada no SQS para o conta processar.
     */
    public ComandoContaRequest toComandoContaRequest(LedgerRequest request, String instanceId) {
        return new ComandoContaRequest(
                request.getHeaderMessageGrpc().getCorrelationId(),
                instanceId,
                request.getContaId(),
                request.getCustomReturnLedger(),
                request.getSleepLedgerEfetivacao()
        );
    }

    /**
     * Resultado real, gravado (COMPLETED) a partir do callback gRPC do conta.
     */
    public ComandoContaEntity toCompletedEntity(RetornoContaRequest retorno) {
        return ComandoContaEntity.builder()
                .correlationId(retorno.getCorrelationId())
                .approved(retorno.getApproved())
                .errorCode(retorno.getErrorCode())
                .errorDescription(retorno.getErrorDescription())
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }
}
