package br.com.mmgabri.services;

import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.domains.FormatadorResponse;
import br.com.mmgabri.domains.HeaderMessage;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.AutorizadorResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MapperService {
    private static final Logger logger = LoggerFactory.getLogger(MapperService.class);

    public AutorizadorRequest toAutorizadorRequest(FormatadorRequest request) {
        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setCorrelationId(UUID.randomUUID().toString())
                .setBandeira("Mastercard")
                .setPlataforma("SINGLE_MESSAGE")
                .setTimestamp(LocalDateTime.now().toString())
                .build();

        AutorizadorRequest autorizadorRequest = AutorizadorRequest.newBuilder()
                .setHeaderMessageGrpc(header)
                .putAllMessageIso(request.getMessageIso())
                .setCustomReturnDataEnrichment(request.getCustomReturnDataEnrichment())
                .setSleepDataEnrichment(request.getSleepDataEnrichment())
                .setCustomReturnRules(request.getCustomReturnRules())
                .setSleepRules(request.getSleepRules())
                .setCustomReturnSeguranca(request.getCustomReturnSeguranca())
                .setSleepSeguranca(request.getSleepSeguranca())
                .setCustomReturnLimit(request.getCustomReturnLimit())
                .setSleepLimitEfetivacao(request.getSleepLimitEfetivacao())
                .setSleepLimitSimulacao(request.getSleepLimitSimulacao())
                .setCustomReturnLedger(request.getCustomReturnLedger())
                .setSleepLedgerEfetivacao(request.getSleepLedgerEfetivacao())
                .setSleepLedgerSimulacao(request.getSleepLedgerSimulacao())
                .setCustomReturnFraude(request.getCustomReturnFraude())
                .setSleepFraude(request.getSleepFraude())
                .setTransactionIdReversal(request.getTransactionIdReversal() != null ? request.getTransactionIdReversal() : "")
                .build();
        return autorizadorRequest;
    }

    public FormatadorResponse toReversalResponse(FormatadorRequest request) {
        Map<String, String> iso = request.getMessageIso();
        Map<String, String> messageIso = new HashMap<>(iso);
        messageIso.put("039", "00");

        HeaderMessage header = HeaderMessage.builder()
                .transactionId(request.getTransactionIdReversal())
                .correlationId(UUID.randomUUID().toString())
                .bandeira("MASTERCARD")
                .plataforma("SINGLE_MESSAGE")
                .timestamp(LocalDateTime.now().toString())
                .message("Reversal solicitado com sucesso")
                .isReversal(true)
                .build();

        return FormatadorResponse.builder()
                .header(header)
                .messageIso(messageIso)
                .build();
    }

    public FormatadorResponse toFormatadorResponse(AutorizadorResponse response) {
        HeaderMessage header = HeaderMessage.builder()
                .transactionId(response.getHeaderMessageGrpc().getTransactionId())
                .correlationId(response.getHeaderMessageGrpc().getCorrelationId())
                .bandeira(response.getHeaderMessageGrpc().getBandeira())
                .plataforma(response.getHeaderMessageGrpc().getPlataforma())
                .timestamp(response.getHeaderMessageGrpc().getTimestamp())
                .message(response.getHeaderMessageGrpc().getMessage())
                .isReversal(response.getHeaderMessageGrpc().getIsReversal())
                .build();

        FormatadorResponse formatadorResponse = FormatadorResponse.builder()
                .header(header)
                .messageIso(response.getMessageIsoMap())
                .build();
        return formatadorResponse;
    }
}
