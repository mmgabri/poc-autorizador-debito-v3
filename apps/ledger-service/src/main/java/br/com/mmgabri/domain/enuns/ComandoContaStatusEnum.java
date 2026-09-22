package br.com.mmgabri.domain.enuns;

import lombok.Getter;

/**
 * Estados do registro de efetivação assíncrona (tabela comando_conta) — agora
 * uma trilha de auditoria de 3 pontos fixos, todos escritos por este serviço
 * (ledger-service), nunca em disputa entre processos diferentes:
 * <p>
 * PENDING (antes de publicar no SQS) → COMPLETED (ao receber o callback gRPC
 * do conta, antes do PUBLISH no Redis) → COMPLETED_ACK (na própria instância,
 * depois de consumir a notificação via SUB e destravar o gRPC bloqueado).
 */
@Getter
public enum ComandoContaStatusEnum {
    PENDING(false, "Comando registrado, publicado no SQS para o conta, gRPC do autorizador bloqueado aguardando"),
    COMPLETED(false, "Conta respondeu via callback gRPC; resultado gravado, sinal publicado no Redis (transitório)"),
    COMPLETED_ACK(true, "Instância que despachou consumiu o sinal via pub/sub e destravou o gRPC do autorizador");

    private final boolean terminal;
    private final String description;

    ComandoContaStatusEnum(boolean terminal, String description) {
        this.terminal = terminal;
        this.description = description;
    }
}
