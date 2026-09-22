package br.com.mmgabri.adapters.grpc.server;

import br.com.mmgabri.adapters.dynamodb.mapper.ComandoContaMapper;
import br.com.mmgabri.adapters.dynamodb.repository.ComandoContaRepository;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisPublisher;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaRequest;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaResponse;
import br.com.mmgabri.grpc.retornoconta.v1.RetornoContaServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

/**
 * Recebe o callback do conta (passo PUB): grava o resultado real (COMPLETED)
 * e publica no canal Redis da instância que despachou (roteado por
 * instanceId, já que essa chamada pode cair em qualquer réplica do
 * ledger-service via load balancing do Service do k8s).
 */
@GrpcService
@RequiredArgsConstructor
public class RetornoContaControllerGrpc extends RetornoContaServiceGrpc.RetornoContaServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(RetornoContaControllerGrpc.class);

    private final ComandoContaRepository comandoContaRepository;
    private final ComandoContaMapper comandoContaMapper;
    private final LedgerCompletionRedisPublisher redisPublisher;

    @Override
    public void trataRetornoConta(RetornoContaRequest request, StreamObserver<RetornoContaResponse> responseObserver) {
        logger.debug("Callback recebido do conta. correlationId={} instanceId={}", request.getCorrelationId(), request.getInstanceId());

        var completedEntity = comandoContaMapper.toCompletedEntity(request);
        comandoContaRepository.updateCompleted(completedEntity);
        redisPublisher.publish(request);

        responseObserver.onNext(RetornoContaResponse.newBuilder().setMessage("OK").build());
        responseObserver.onCompleted();
    }
}
