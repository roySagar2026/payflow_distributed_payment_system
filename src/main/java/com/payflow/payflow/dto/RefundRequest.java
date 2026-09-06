package com.payflow.payflow.dto;

import jakarta.validation.constraints.NotBlank;

public record RefundRequest(
        @NotBlank String reason
) {}