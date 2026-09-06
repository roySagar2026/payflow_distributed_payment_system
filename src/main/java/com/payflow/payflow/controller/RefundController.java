package com.payflow.payflow.controller;

import com.payflow.payflow.audit.Audited;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.service.RefundService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RefundController {

    private final RefundService refundService;

    @Audited(action = "REFUND_ISSUED", resourceType = "Refund")
    @PostMapping("/{orderId}/refund")
    public ResponseEntity<RefundResponse> refund(
            @PathVariable UUID orderId,
            @Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(refundService.refund(orderId, request));
    }
}