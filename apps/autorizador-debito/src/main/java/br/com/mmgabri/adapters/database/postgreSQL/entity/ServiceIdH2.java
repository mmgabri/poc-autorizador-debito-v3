package br.com.mmgabri.adapters.database.postgreSQL.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceIdH2 implements Serializable {
    private String transactionId;
    private String service;
}