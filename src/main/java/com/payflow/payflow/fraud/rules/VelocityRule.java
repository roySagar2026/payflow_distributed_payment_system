package com.payflow.payflow.fraud.rules;

import com.payflow.payflow.model.PaymentOrder;
import com.payflow.payflow.fraud.FraudRule;
import com.payflow.payflow.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class VelocityRule implements FraudRule {

    private final PaymentOrderRepository paymentOrderRepository;

    private static final int MAX_ORDERS_PER_MINUTE = 5;

    @Override
    public RuleResult evaluate(PaymentOrder order) {
        Instant oneMinuteAgo = Instant.now().minus(1, ChronoUnit.MINUTES);

        long recentCount = paymentOrderRepository.countByPayerWalletIdAndCreatedAtAfter(
                order.getPayerWalletId(), oneMinuteAgo);

        if (recentCount > MAX_ORDERS_PER_MINUTE) {
            return new RuleResult(50, "More than " + MAX_ORDERS_PER_MINUTE + " payments in the last minute");
        }
        return RuleResult.clean();
    }
}