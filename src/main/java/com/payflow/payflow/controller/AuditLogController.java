package com.payflow.payflow.controller;

import com.payflow.payflow.model.AuditLog;
import com.payflow.payflow.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/me")
    public ResponseEntity<List<AuditLog>> myAuditLogs(Authentication auth) {
        UUID userId = (UUID) auth.getPrincipal();
        return ResponseEntity.ok(auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }
}