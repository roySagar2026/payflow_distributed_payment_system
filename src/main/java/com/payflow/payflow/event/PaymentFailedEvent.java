package com.payflow.payflow.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentFailedEvent(
        UUID orderId,
        UUID payerWalletId,
        UUID payeeWalletId,
        BigDecimal amount,
        String failureReason
) {}