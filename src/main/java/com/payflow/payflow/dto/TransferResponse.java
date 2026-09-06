package com.payflow.payflow.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        UUID senderWalletId,
        UUID receiverWalletId,
        BigDecimal amount,
        BigDecimal senderBalanceAfter
) {}