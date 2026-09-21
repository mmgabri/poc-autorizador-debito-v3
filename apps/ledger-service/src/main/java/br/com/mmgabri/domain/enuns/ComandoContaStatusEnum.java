package br.com.mmgabri.domain.enuns;

import lombok.Getter;

/**
 * Estados do registro de efetivação assíncrona (tabela comando_conta).
 * <p>
 * PENDING e TIMEOUT são escritos pelo autorizador-debito; COMPLETED é escrito
 * por este serviço; COMPLETED_LATE pode ser escrito por qualquer um dos dois,
 * dependendo de quem perde a corrida na transição atômica (compare-and-swap)
 * no DynamoDB.
 */
@Getter
public enum ComandoContaStatusEnum {
    PENDING(false, "Comando registrado, thread do autorizador bloqueada no BLPOP"),
    TIMEOUT(false, "BLPOP expirou no autorizador antes do ledger concluir"),
    COMPLETED(false, "Ledger concluiu e sinalizou enquanto o autorizador ainda esperava (transitório)"),
    COMPLETED_LATE(true, "Ledger concluiu, mas o autorizador já tinha desistido - resposta órfã, não usada"),
    COMPLETED_ACK(true, "Autorizador consumiu o payload do BLPOP e seguiu com o resultado real");

    private final boolean terminal;
    private final String description;

    ComandoContaStatusEnum(boolean terminal, String description) {
        this.terminal = terminal;
        this.description = description;
    }
}
