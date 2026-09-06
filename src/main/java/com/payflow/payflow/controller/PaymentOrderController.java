package com.payflow.payflow.controller;

import com.payflow.payflow.dto.*;
import com.payflow.payflow.service.IdempotencyService;
import com.payflow.payflow.service.PaymentOrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment-orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class PaymentOrderController {

    private final PaymentOrderService paymentOrderService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<PaymentOrderResponse> create(
            Authentication auth,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePaymentOrderRequest request) {

        Optional<PaymentOrderResponse> cached = idempotencyService.reserveOrGetCached(
                idempotencyKey, request, PaymentOrderResponse.class);

        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        UUID payerUserId = (UUID) auth.getPrincipal();
        PaymentOrderResponse response = paymentOrderService.createAndProcess(payerUserId, request);
        idempotencyService.markCompleted(idempotencyKey, response);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<PaymentOrderResponse> getById(@PathVariable UUID orderId) {
        return ResponseEntity.ok(paymentOrderService.getById(orderId));
    }
}