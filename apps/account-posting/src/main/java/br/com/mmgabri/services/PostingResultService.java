package br.com.mmgabri.services;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.mmgabri.adapters.dynamodb.mapper.AccountCommandMapper;
import br.com.mmgabri.adapters.dynamodb.repository.AccountCommandRepository;
import br.com.mmgabri.adapters.redis.LedgerCompletionRedisPublisher;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Receives the conta callback (PUB step): stores the actual result (COMPLETED)
 * and publishes it on the Redis channel of the dispatching instance (routed by
 * instanceId, since this call may land on any account-posting replica through
 * the k8s Service load balancing).
 */
@Service
@RequiredArgsConstructor
public class PostingResultService {

    private static final Logger logger = LoggerFactory.getLogger(PostingResultService.class);

    private final AccountCommandRepository accountCommandRepository;
    private final AccountCommandMapper accountCommandMapper;
    private final LedgerCompletionRedisPublisher redisPublisher;

    public void execute(HandlePostingResultRequest request) {
        logger.debug("Callback received from conta. correlationId={} instanceId={}", request.getCorrelationId(), request.getInstanceId());

        var completedEntity = accountCommandMapper.toCompletedEntity(request);
        accountCommandRepository.updateCompleted(completedEntity);
        redisPublisher.publish(request);
    }
}
