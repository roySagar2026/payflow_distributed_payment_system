package com.payflow.payflow.controller;

import com.payflow.payflow.model.Notification;
import com.payflow.payflow.repository.NotificationRepository;
import com.payflow.payflow.repository.WalletRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final WalletRepository walletRepository;

    @GetMapping("/me")
    public ResponseEntity<List<Notification>> myNotifications(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        var wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        return ResponseEntity.ok(
                notificationRepository.findByUserWalletIdOrderByCreatedAtDesc(wallet.getId()));
    }
}