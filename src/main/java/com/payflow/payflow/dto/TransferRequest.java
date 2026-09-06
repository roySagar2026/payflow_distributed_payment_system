package com.payflow.payflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID receiverUserId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount
) {}