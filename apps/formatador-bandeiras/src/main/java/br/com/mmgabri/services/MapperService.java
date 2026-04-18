package br.com.mmgabri.services;

import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.domains.FormatadorResponse;
import br.com.mmgabri.domains.HeaderMessage;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.AutorizadorResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
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
                .setCustomReturnSeguranca(request.getCustomReturnSeguranca())
                .setCustomReturnFraude(request.getCustomReturnFraude())
                .setCustomReturnLancamentoConta(request.getCustomReturnLancamentoConta())
                .setCustomReturnLimite(request.getCustomReturnLimite())
                .setCustomReturnLimitePortador(request.getCustomReturnLimitePortador())
                .setSleepDataEnrichment(request.getSleepDataEnrichment())
                .setSleepSeguranca(request.getSleepSeguranca())
                .setSleepLimitePortador(request.getSleepLimitePortador())
                .setSleepLimite(request.getSleepLimite())
                .setSleepLancamentoConta(request.getSleepLancamentoConta())
                .setSleepFraude(request.getSleepFraude())
                .build();
        return autorizadorRequest;
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
