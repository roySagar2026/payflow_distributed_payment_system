package com.payflow.payflow.audit;

import com.payflow.payflow.model.AuditLog;
import com.payflow.payflow.model.AuditOutcome;
import com.payflow.payflow.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object logAuditedAction(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        UUID userId = extractUserId();
        String ipAddress = extractIpAddress();

        try {
            Object result = joinPoint.proceed();

            saveLog(userId, audited.action(), audited.resourceType(),
                    extractResourceId(result), AuditOutcome.SUCCESS, null, ipAddress);

            return result;

        } catch (Exception e) {
            saveLog(userId, audited.action(), audited.resourceType(),
                    null, AuditOutcome.FAILURE, e.getMessage(), ipAddress);
            throw e;
        }
    }

    private UUID extractUserId() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UUID uuid) {
                return uuid;
            }
        } catch (Exception ignored) {
            // Not authenticated (e.g., failed login attempt) — that's fine, userId stays null
        }
        return null;
    }

    private String extractIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String forwarded = request.getHeader("X-Forwarded-For");
                return forwarded != null ? forwarded : request.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String extractResourceId(Object result) {
        // Best-effort: if the result is a ResponseEntity wrapping a record with a getter
        // like getOrderId()/getId(), we could reflectively extract it. For simplicity here,
        // we just record that an operation completed — you can extend this later.
        return null;
    }

    private void saveLog(UUID userId, String action, String resourceType, String resourceId,
                         AuditOutcome outcome, String details, String ipAddress) {
        AuditLog log = AuditLog.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType.isEmpty() ? null : resourceType)
                .resourceId(resourceId)
                .outcome(outcome)
                .details(details)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(log);
    }
}