package br.com.mmgabri.adapters.grpc.client;

import br.com.mmgabri.adapters.grpc.config.AutorizadorGrpcStubProvider;
import br.com.mmgabri.services.MapperService;
import br.com.mmgabri.domains.FormatadorRequest;
import br.com.mmgabri.grpc.autorizador.v1.AutorizadorResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AutorizadorGrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(AutorizadorGrpcClient.class);

    private final AutorizadorGrpcStubProvider autorizadorGrpcClient;
    private final MapperService mapper;

    @SneakyThrows
    public AutorizadorResponse execute(FormatadorRequest payload) {
        try {
            var request = mapper.toAutorizadorRequest(payload);
            logger.debug("Starting autorizador call");
            AutorizadorResponse response = autorizadorGrpcClient.getStub().autorizarTransacao(request);
            logger.debug("Autorizador call succeeded");
            return response;
        } catch (Exception e) {
            logger.error("Autorizador call failed", e);
            throw e;
        }
    }
}