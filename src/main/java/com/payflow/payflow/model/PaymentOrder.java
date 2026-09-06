package com.payflow.payflow.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID payerWalletId;

    @Column(nullable = false)
    private UUID payeeWalletId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentOrderStatus status;

    @Column
    private String failureReason;

    @Column
    private UUID transferId;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = PaymentOrderStatus.CREATED;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Enforces valid state transitions. This is the heart of the state machine —
     * it's what prevents invalid jumps like CREATED -> COMPLETED.
     */
    public void transitionTo(PaymentOrderStatus newStatus) {
        boolean valid = switch (this.status) {
            case CREATED -> newStatus == PaymentOrderStatus.PENDING || newStatus == PaymentOrderStatus.FAILED;
            case PENDING -> newStatus == PaymentOrderStatus.PROCESSING || newStatus == PaymentOrderStatus.FAILED;
            case PROCESSING -> newStatus == PaymentOrderStatus.COMPLETED || newStatus == PaymentOrderStatus.FAILED;
            case COMPLETED -> newStatus == PaymentOrderStatus.REFUNDED;
            case FAILED, REFUNDED -> false; // terminal states — no further transitions
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid payment order transition: " + this.status + " -> " + newStatus);
        }

        this.status = newStatus;
    }
}