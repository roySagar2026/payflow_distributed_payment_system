package com.payflow.payflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Column
    private String resourceType;

    @Column
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}