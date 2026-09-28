package br.com.mmgabri.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Command consumed from SQS, published by account-posting.
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
