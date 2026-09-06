package com.payflow.payflow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentOrderResponse(
        UUID orderId,
        String status,
        BigDecimal amount,
        UUID transferId,
        String failureReason,
        Instant createdAt
) {}