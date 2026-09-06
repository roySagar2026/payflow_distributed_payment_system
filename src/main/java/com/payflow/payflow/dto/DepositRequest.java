package com.payflow.payflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record DepositRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive") BigDecimal amount
) {}