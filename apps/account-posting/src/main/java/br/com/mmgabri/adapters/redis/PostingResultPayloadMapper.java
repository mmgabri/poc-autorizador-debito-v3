package br.com.mmgabri.adapters.redis;

import br.com.itau.debit.authorizer.accountposting.v1.HandlePostingResultRequest;
import br.com.itau.debit.authorizer.common.v1.BusinessResult;
import br.com.itau.debit.authorizer.common.v1.ReasonCode;
import br.com.itau.debit.authorizer.common.v1.TechnicalError;
import br.com.mmgabri.errors.GrpcErrors;

/**
 * Conversion HandlePostingResultRequest (proto, with oneof) ⇄ PostingResultPayload
 * (Redis channel JSON).
 */
public final class PostingResultPayloadMapper {

    private PostingResultPayloadMapper() {
    }

    public static PostingResultPayload toPayload(HandlePostingResultRequest postingResult) {
        if (postingResult.hasTechnicalError()) {
            var technicalError = postingResult.getTechnicalError();
            var code = GrpcErrors.codeOf(technicalError.getReasonCode());
            return new PostingResultPayload(postingResult.getCorrelationId(), true, false, code, technicalError.getMessage());
        }

        var result = postingResult.getBusinessResult();
        var code = result.getApproved() ? "" : GrpcErrors.codeOf(result.getReasonCode());
        return new PostingResultPayload(postingResult.getCorrelationId(), false, result.getApproved(), code, result.getMessage());
    }

    public static HandlePostingResultRequest toHandlePostingResultRequest(PostingResultPayload payload) {
        var builder = HandlePostingResultRequest.newBuilder()
                .setCorrelationId(payload.correlationId());
        var message = payload.message() == null ? "" : payload.message();

        if (payload.technicalError()) {
            var reasonCode = resolve(payload.reasonCode(), ReasonCode.REASON_CODE_ACCOUNT_POSTING_INTERNAL_ERROR);
            var technicalError = TechnicalError.newBuilder()
                    .setReasonCode(reasonCode)
                    .setMessage(message)
                    .build();
            return builder.setTechnicalError(technicalError).build();
        }

        var resultBuilder = BusinessResult.newBuilder()
                .setApproved(payload.approved())
                .setMessage(message);
        if (!payload.approved()) {
            var reasonCode = resolve(payload.reasonCode(), ReasonCode.REASON_CODE_UNSPECIFIED);
            resultBuilder.setReasonCode(reasonCode);
        }
        var result = resultBuilder.build();
        return builder.setBusinessResult(result).build();
    }

    private static ReasonCode resolve(String code, ReasonCode fallback) {
        var reasonCode = GrpcErrors.fromCode(code);
        return reasonCode.orElse(fallback);
    }
}
