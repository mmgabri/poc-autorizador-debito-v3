package br.com.mmgabri.application.domains.enuns;

import lombok.Getter;

// Motivo pelo qual a efetivação assíncrona do ledger está sendo confirmada no
// DynamoDB — usado só pra deixar autoexplicativo o warning de inconsistência
// quando o registro não confirma "completed".
@Getter
public enum CompletionTriggerEnum {
    REDIS_SIGNAL("Sinal recebido do ledger"),
    LOST_TIMEOUT_RACE("TIMEOUT perdeu a corrida");

    private final String description;

    CompletionTriggerEnum(String description) {
        this.description = description;
    }
}
