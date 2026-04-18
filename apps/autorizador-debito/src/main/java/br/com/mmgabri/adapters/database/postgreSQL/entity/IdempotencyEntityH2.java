package br.com.mmgabri.adapters.database.postgreSQL.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;


@Entity
@Table(name = "idempotency")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class IdempotencyEntityH2 {
    @Id
    private String correlationId;

    @Column(length = 36)
    private String transactionId;

    @Column(length = 50)
    private String executedAt;
}