package br.com.mmgabri.adapters.database.postgreSQL.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "service_executed_context")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ServiceContextEntityH2 {
    @EmbeddedId
    private ServiceIdH2 serviceId;

    @Column(length = 100)
    private String status;

    @Column(length = 30)
    private String errorCode;

    @Column(length = 300)
    private String errorDescription;

    @Column(length = 50)
    private String executedAt;

    @Column(length = 50)
    private String reversedAt;
}