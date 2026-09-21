package br.com.mmgabri.adapters.dynamodb.mapper;

import br.com.mmgabri.adapters.dynamodb.entity.ComandoContaEntity;
import br.com.mmgabri.domain.ComandoContaRequest;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Constrói o registro de resultado da efetivação (comando_conta) a partir da
 * mensagem recebida via SQS.
 */
@Service
public class ComandoContaMapper {

    public ComandoContaEntity toResultFields(ComandoContaRequest payload, boolean approved, String errorDescription) {
        return ComandoContaEntity.builder()
                .correlationId(payload.correlationId())
                .contaId(payload.contaId())
                .approved(approved)
                .errorCode(payload.customReturnLedger())
                .errorDescription(errorDescription)
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }
}
