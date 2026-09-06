package com.payflow.payflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_checks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID paymentOrderId;

    @Column(nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudDecision decision;

    @Column(columnDefinition = "TEXT")
    private String triggeredRules;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}