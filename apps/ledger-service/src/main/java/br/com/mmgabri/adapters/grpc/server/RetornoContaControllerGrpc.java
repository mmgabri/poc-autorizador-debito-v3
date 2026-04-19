package br.com.mmgabri.adapters.grpc.server;

import br.com.mmgabri.adapters.redis.RetornoContaRedisPublisher;
import br.com.mmgabri.domain.RedisCallbackMessage;
import br.com.mmgabri.grpc.RetornoContaRequest;
import br.com.mmgabri.grpc.RetornoContaResponse;
import br.com.mmgabri.grpc.RetornoContaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class RetornoContaControllerGrpc extends RetornoContaServiceGrpc.RetornoContaServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RetornoContaControllerGrpc.class);

    private final RetornoContaRedisPublisher redisPublisher;

    @Override
    public void trataRetornoConta(RetornoContaRequest request, StreamObserver<RetornoContaResponse> responseObserver) {
        logger.debug("Callback TrataRetornoConta recebido. correlationId={} instanceId={}", request.getCorrelationId(), request.getInstanceId());
        try {
            RedisCallbackMessage message = new RedisCallbackMessage(
                    request.getCorrelationId(),
                    request.getApproved(),
                    request.getErrorCode(),
                    request.getErrorDescription()
            );
            redisPublisher.publish(request.getInstanceId(), message);

            responseObserver.onNext(RetornoContaResponse.newBuilder().setMessage("OK").build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Erro ao processar TrataRetornoConta. correlationId={}", request.getCorrelationId(), e);
            responseObserver.onError(
                    io.grpc.Status.INTERNAL.withDescription("Erro interno").asRuntimeException()
            );
        }
    }
}
