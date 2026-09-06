package com.payflow.payflow.controller;

import com.payflow.payflow.audit.Audited;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.ratelimit.RateLimit;
import com.payflow.payflow.service.IdempotencyService;
import com.payflow.payflow.service.TransferService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;
    private final IdempotencyService idempotencyService;

    @Audited(action = "TRANSFER_MONEY", resourceType = "Transfer")
    @RateLimit(maxRequests = 10, windowSeconds = 60)
    @PostMapping
    public ResponseEntity<TransferResponse> transfer(
            Authentication auth,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        Optional<TransferResponse> cached = idempotencyService.reserveOrGetCached(
                idempotencyKey, request, TransferResponse.class);

        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        UUID senderUserId = (UUID) auth.getPrincipal();
        TransferResponse response = transferService.transfer(senderUserId, request);
        idempotencyService.markCompleted(idempotencyKey, response);

        return ResponseEntity.ok(response);
    }
}