package br.com.mmgabri.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Command published to SQS for conta to execute the account posting.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountCommand(
        String correlationId,
        String instanceId,
        String accountId,
        String customReturn,
        long sleepCommit
) {
}
