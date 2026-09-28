package br.com.mmgabri.adapters.redis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Body of the message published/consumed on the pub/sub channel
 * {@code efetivacao:conta:{instanceId}} - the correlationId identifies which local
 * future to complete, since the channel is fixed per instance (not per transaction).
 * <p>
 * Mirrors the oneof of HandlePostingResultRequest: {@code technicalError=true} when
 * conta reported a technical failure; otherwise it is a business result (approved /
 * declined). {@code reasonCode} is the catalog code, e.g. "AC-B001" (empty when approved).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PostingResultPayload(
        String correlationId,
        boolean technicalError,
        boolean approved,
        String reasonCode,
        String message
) {
}
