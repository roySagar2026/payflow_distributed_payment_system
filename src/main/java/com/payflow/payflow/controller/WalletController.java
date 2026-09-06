package com.payflow.payflow.controller;

import com.payflow.payflow.audit.Audited;
import com.payflow.payflow.dto.*;
import com.payflow.payflow.ratelimit.RateLimit;
import com.payflow.payflow.service.IdempotencyService;
import com.payflow.payflow.service.WalletService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final WalletService walletService;
    private final IdempotencyService idempotencyService;

    @Audited(action = "WALLET_DEPOSIT", resourceType = "Wallet")
    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            Authentication auth,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody DepositRequest request) {

        Optional<WalletResponse> cached = idempotencyService.reserveOrGetCached(
                idempotencyKey, request, WalletResponse.class);

        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        UUID userId = (UUID) auth.getPrincipal();
        WalletResponse response = walletService.deposit(userId, request);
        idempotencyService.markCompleted(idempotencyKey, response);

        return ResponseEntity.ok(response);
    }

    @Audited(action = "WALLET_WITHDRAW", resourceType = "Wallet")
    @RateLimit(maxRequests = 5, windowSeconds = 60)
    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            Authentication auth,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody WithdrawRequest request) {

        Optional<WalletResponse> cached = idempotencyService.reserveOrGetCached(
                idempotencyKey, request, WalletResponse.class);

        if (cached.isPresent()) {
            return ResponseEntity.ok(cached.get());
        }

        UUID userId = (UUID) auth.getPrincipal();
        WalletResponse response = walletService.withdraw(userId, request);
        idempotencyService.markCompleted(idempotencyKey, response);

        return ResponseEntity.ok(response);
    }
}