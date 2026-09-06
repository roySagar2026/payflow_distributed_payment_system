package com.payflow.payflow.repository;

import com.payflow.payflow.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID> {
    long countByPayerWalletIdAndCreatedAtAfter(UUID payerWalletId, Instant after);
}