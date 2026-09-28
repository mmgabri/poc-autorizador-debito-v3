package br.com.mmgabri.adapters.grpc.client;

import br.com.itau.debit.authorizer.debitauthorizer.v1.AuthorizeTransactionResponse;
import br.com.mmgabri.adapters.grpc.config.AuthorizerGrpcStubProvider;
import br.com.mmgabri.domains.AuthorizationRequest;
import br.com.mmgabri.services.MapperService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthorizerGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizerGrpcClient.class);

    private final AuthorizerGrpcStubProvider authorizerGrpcStubProvider;
    private final MapperService mapper;

    @SneakyThrows
    public AuthorizeTransactionResponse execute(AuthorizationRequest payload, String correlationId) {
        try {
            var request = mapper.toAuthorizeTransactionRequest(payload, correlationId);
            logger.debug("Starting debit-authorizer call. correlationId={}", correlationId);
            var stub = authorizerGrpcStubProvider.getStub(correlationId);
            AuthorizeTransactionResponse response = stub.authorizeTransaction(request);
            logger.debug("debit-authorizer call succeeded");
            return response;
        } catch (Exception e) {
            logger.error("debit-authorizer call failed", e);
            throw e;
        }
    }
}
