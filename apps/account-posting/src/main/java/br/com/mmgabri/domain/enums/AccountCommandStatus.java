package br.com.mmgabri.domain.enums;

import lombok.Getter;

/**
 * States of the asynchronous commit record (comando_conta table) - an audit trail
 * of 3 fixed points, all written by this service (account-posting), never contended
 * by different processes:
 * <p>
 * PENDING (before publishing to SQS) → COMPLETED (when the conta gRPC callback
 * arrives, before the Redis PUBLISH) → COMPLETED_ACK (on the dispatching instance,
 * after consuming the notification via SUB and releasing the blocked gRPC call).
 */
@Getter
public enum AccountCommandStatus {
    PENDING(false, "Command registered and published to SQS for conta; authorizer gRPC call blocked waiting"),
    COMPLETED(false, "Conta answered via gRPC callback; result stored, signal published to Redis (transient)"),
    COMPLETED_ACK(true, "Dispatching instance consumed the signal via pub/sub and released the authorizer gRPC call");

    private final boolean terminal;
    private final String description;

    AccountCommandStatus(boolean terminal, String description) {
        this.terminal = terminal;
        this.description = description;
    }
}
