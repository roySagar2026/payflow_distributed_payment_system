package com.payflow.payflow.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundResponse(
        UUID refundId,
        UUID originalPaymentOrderId,
        BigDecimal amount,
        String status
) {}