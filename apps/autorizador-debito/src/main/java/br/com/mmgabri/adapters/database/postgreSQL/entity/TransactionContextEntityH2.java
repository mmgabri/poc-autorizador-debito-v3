package br.com.mmgabri.adapters.database.postgreSQL.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;


@Entity
@Table(name = "transaction_context")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TransactionContextEntityH2 {
    @Id
    private String transactionId;

    @Column(length = 100)
    private String status;

    @Column(length = 30000)
    private String payload;

    @Column(length = 50)
    private String createdAt;

    @Column(length = 50)
    private String reversedAt;
}