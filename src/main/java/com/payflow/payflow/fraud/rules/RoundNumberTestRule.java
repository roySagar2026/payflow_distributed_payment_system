package com.payflow.payflow.fraud.rules;

import com.payflow.payflow.model.PaymentOrder;
import com.payflow.payflow.fraud.FraudRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RoundNumberTestRule implements FraudRule {

    private static final BigDecimal SUSPICIOUS_SMALL_AMOUNT = new BigDecimal("1");

    @Override
    public RuleResult evaluate(PaymentOrder order) {
        // Very small "test" transactions (like ₹1) are a classic pattern for
        // attackers probing whether a stolen wallet/card is "live" before a bigger attempt.
        if (order.getAmount().compareTo(SUSPICIOUS_SMALL_AMOUNT) <= 0) {
            return new RuleResult(15, "Suspiciously small 'test' amount");
        }
        return RuleResult.clean();
    }
}