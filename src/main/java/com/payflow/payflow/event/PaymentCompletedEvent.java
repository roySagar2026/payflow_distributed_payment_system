package com.payflow.payflow.event;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCompletedEvent(
        UUID orderId,
        UUID payerWalletId,
        UUID payeeWalletId,
        BigDecimal amount,
        UUID transferId
) {}