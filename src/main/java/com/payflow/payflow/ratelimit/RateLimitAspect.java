package com.payflow.payflow.ratelimit;

import com.payflow.payflow.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimiterService rateLimiterService;

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        UUID userId = (UUID) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String key = "ratelimit:" + signature.getMethod().getName() + ":" + userId;

        boolean allowed = rateLimiterService.isAllowed(
                key, rateLimit.maxRequests(), Duration.ofSeconds(rateLimit.windowSeconds()));

        if (!allowed) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded: max " + rateLimit.maxRequests() +
                            " requests per " + rateLimit.windowSeconds() + " seconds");
        }

        return joinPoint.proceed();
    }
}