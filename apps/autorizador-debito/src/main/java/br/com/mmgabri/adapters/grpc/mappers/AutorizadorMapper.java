package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.mmgabri.grpc.AutorizadorRequest;
import br.com.mmgabri.grpc.AutorizadorResponse;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class AutorizadorMapper {
    private static final Logger logger = LoggerFactory.getLogger(AutorizadorMapper.class);

    //  private final ErrorDecodingService errorDecodingService;

    public AutorizadorResponse toAutorizadorResponseSuccess(Payload payload) {
        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage("Transação processada com sucesso!")
                .build();

        AutorizadorResponse response = AutorizadorResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .putAllMessageIso(buildMessageResponseIso(payload, "00"))
                .build();
        return response;
    }

    public AutorizadorResponse toAutorizadorResponseError(Payload payload, Throwable throwable) {

        //ErrorDetails errorDetails = errorDecodingService.execute(throwable);

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .setCorrelationId(payload.getHeaderMessage().getCorrelationId())
                .setBandeira(payload.getHeaderMessage().getBandeira())
                .setPlataforma(payload.getHeaderMessage().getPlataforma())
                .setTimestamp(payload.getHeaderMessage().getTimestamp())
                .setMessage(throwable.getMessage())
                .build();

        AutorizadorResponse response = AutorizadorResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .putAllMessageIso(buildMessageResponseIso(payload, "96"))
                .build();
        return response;
    }

    public AutorizadorResponse toAutorizadorResponseError(AutorizadorRequest request, Throwable throwable) {

        // ErrorDetails errorDetails = errorDecodingService.execute(throwable);

        HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                .setCorrelationId(request.getHeaderMessageGrpc().getCorrelationId())
                .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                .setMessage(throwable.getMessage())
                .build();

        AutorizadorResponse response = AutorizadorResponse.newBuilder()
                .setHeaderMessageGrpc(header)
                .putAllMessageIso(buildMessageResponseIso(request, "96"))
                .build();
        return response;
    }

    private Map buildMessageResponseIso(Payload payload, String de39) {
        int mti = Integer.parseInt(payload.getMessageIso().get("mti"));
        mti = mti +10;

        Map<String, String> isoResp = new java.util.HashMap<>(payload.getMessageIso());
        isoResp.put("039", de39);
        isoResp.put("mti", String.format("%04d", mti));
        return isoResp;
    }

    private Map buildMessageResponseIso(AutorizadorRequest request, String de39) {
        int mti = Integer.parseInt(request.getMessageIso().get("mti"));
        mti = mti +10;

        Map<String, String> isoResp = new java.util.HashMap<>(request.getMessageIso());
        isoResp.put("039", de39);
        isoResp.put("mti", String.format("%04d", mti));
        return isoResp;
    }


}
