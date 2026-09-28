package br.com.mmgabri.adapters.grpc.mappers;

import br.com.mmgabri.application.domains.Payload;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionRequest;
import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class AuthorizationMapper {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMapper.class);

    //  private final ErrorDecodingService errorDecodingService;

    public AuthorizeTransactionResponse toSuccessResponse(Payload payload) {
        AuthorizeTransactionResponse response = AuthorizeTransactionResponse.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .putAllMessageIso(buildMessageResponseIso(payload, "00"))
                .build();
        return response;
    }

    public AuthorizeTransactionResponse toErrorResponse(Payload payload, Throwable throwable) {

        //ErrorDetails errorDetails = errorDecodingService.execute(throwable);

        AuthorizeTransactionResponse response = AuthorizeTransactionResponse.newBuilder()
                .setTransactionId(payload.getHeaderMessage().getTransactionId())
                .putAllMessageIso(buildMessageResponseIso(payload, "96"))
                .build();
        return response;
    }

    public AuthorizeTransactionResponse toErrorResponse(AuthorizeTransactionRequest request, Throwable throwable) {

        // ErrorDetails errorDetails = errorDecodingService.execute(throwable);

        AuthorizeTransactionResponse response = AuthorizeTransactionResponse.newBuilder()
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

    private Map buildMessageResponseIso(AuthorizeTransactionRequest request, String de39) {
        int mti = Integer.parseInt(request.getMessageIso().get("mti"));
        mti = mti +10;

        Map<String, String> isoResp = new java.util.HashMap<>(request.getMessageIso());
        isoResp.put("039", de39);
        isoResp.put("mti", String.format("%04d", mti));
        return isoResp;
    }


}
