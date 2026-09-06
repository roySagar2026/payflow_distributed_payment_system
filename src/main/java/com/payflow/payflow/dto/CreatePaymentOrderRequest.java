package com.payflow.payflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentOrderRequest(
        @NotNull UUID payeeUserId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}