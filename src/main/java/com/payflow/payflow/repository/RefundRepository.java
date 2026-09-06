package com.payflow.payflow.repository;

import com.payflow.payflow.model.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface RefundRepository extends JpaRepository<Refund, UUID> {
    List<Refund> findByOriginalPaymentOrderId(UUID orderId);
}