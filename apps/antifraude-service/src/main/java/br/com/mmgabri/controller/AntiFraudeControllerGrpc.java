package br.com.mmgabri.controller;

import br.com.mmgabri.application.domains.FraudesRequest;
import br.com.mmgabri.application.domains.FraudesResponse;
import br.com.mmgabri.application.services.MotorFraudesService;
import br.com.mmgabri.grpc.AntiFraudeRequest;
import br.com.mmgabri.grpc.AntiFraudeResponse;
import br.com.mmgabri.grpc.AntiFraudeServiceGrpc;
import br.com.mmgabri.grpc.comuns.HeaderMessageGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class AntiFraudeControllerGrpc extends AntiFraudeServiceGrpc.AntiFraudeServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(AntiFraudeControllerGrpc.class);

    private final MotorFraudesService motorFraudesService;

    @Override
    public void validarFraude(AntiFraudeRequest request, StreamObserver<AntiFraudeResponse> responseObserver) {
        long startTime = System.nanoTime();
        logger.info("Received validarFraude. transactionId={}", request.getHeaderMessageGrpc().getTransactionId());
        try {
            FraudesRequest frauRequest = FraudesRequest.builder()
                    .transactionId(request.getHeaderMessageGrpc().getTransactionId())
                    .contaId(request.getContaId())
                    .valor(request.getValor())
                    .tipoPessoa(request.getTipoPessoa())
                    .productName(request.getProductName())
                    .customReturnFraude(request.getCustomReturnFraude())
                    .sleepFraude(request.getSleepFraude())
                    .build();

            FraudesResponse frauResponse = motorFraudesService.execute(frauRequest);

            HeaderMessageGrpc header = HeaderMessageGrpc.newBuilder()
                    .setTransactionId(request.getHeaderMessageGrpc().getTransactionId())
                    .setCorrelationId(request.getHeaderMessageGrpc().getCorrelationId())
                    .setBandeira(request.getHeaderMessageGrpc().getBandeira())
                    .setPlataforma(request.getHeaderMessageGrpc().getPlataforma())
                    .setTimestamp(request.getHeaderMessageGrpc().getTimestamp())
                    .build();

            AntiFraudeResponse response = AntiFraudeResponse.newBuilder()
                    .setHeaderMessageGrpc(header)
                    .setAprovado(frauResponse.isAprovado())
                    .setErrorCode(frauResponse.getErrorCode())
                    .setErrorDescription(frauResponse.getErrorDescription())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

            long ms = (System.nanoTime() - startTime) / 1_000_000;
            logger.info("validarFraude concluído em {} ms", ms);

        } catch (RuntimeException e) {
            logger.error("Erro em validarFraude", e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL
                            .withDescription("Erro interno no antifraude")
                            .asRuntimeException()
            );
        }
    }
}
