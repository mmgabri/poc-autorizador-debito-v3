package br.com.mmgabri.adapters.dynamodb.mapper;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.accountposting.v1.RequestPostingRequest;
import br.com.mmgabri.adapters.dynamodb.entity.AccountCommandEntity;
import br.com.mmgabri.adapters.redis.PostingResultPayloadMapper;
import br.com.mmgabri.domain.AccountCommand;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AccountCommandMapper {

    /**
     * Initial record (PENDING), stored before publishing to SQS for conta.
     */
    public AccountCommandEntity toPendingEntity(RequestPostingRequest request, String instanceId) {
        return AccountCommandEntity.builder()
                .correlationId(request.getCorrelationId())
                .accountId(request.getAccountId())
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }

    /**
     * Message published to SQS for conta to process.
     */
    public AccountCommand toAccountCommand(RequestPostingRequest request, String instanceId) {
        return new AccountCommand(
                request.getCorrelationId(),
                instanceId,
                request.getAccountId(),
                request.getCustomReturn(),
                request.getSleepCommit()
        );
    }

    /**
     * Actual result, stored (COMPLETED) from the conta gRPC callback.
     */
    public AccountCommandEntity toCompletedEntity(HandlePostingResultRequest postingResult) {
        // Same JSON as the Redis channel: technicalError / approved / reasonCode (catalog code) / message.
        var payload = PostingResultPayloadMapper.toPayload(postingResult);
        return AccountCommandEntity.builder()
                .correlationId(postingResult.getCorrelationId())
                .approved(payload.approved())
                .errorCode(payload.reasonCode())
                .errorDescription(payload.message())
                .updatedAt(OffsetDateTime.now().toString())
                .build();
    }
}
