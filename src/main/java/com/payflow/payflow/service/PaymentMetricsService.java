package com.payflow.payflow.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMetricsService {

    private final MeterRegistry meterRegistry;

    public void recordPaymentCompleted() {
        meterRegistry.counter("payflow.payments.completed").increment();
    }

    public void recordPaymentFailed(String reason) {
        meterRegistry.counter("payflow.payments.failed", "reason", reason).increment();
    }

    public void recordFraudRejection() {
        meterRegistry.counter("payflow.fraud.rejections").increment();
    }
}