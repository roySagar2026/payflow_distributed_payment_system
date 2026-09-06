package com.payflow.payflow.fraud.rules;

import com.payflow.payflow.model.PaymentOrder;
import com.payflow.payflow.fraud.FraudRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class LargeAmountRule implements FraudRule {

    private static final BigDecimal HIGH_AMOUNT_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal VERY_HIGH_AMOUNT_THRESHOLD = new BigDecimal("200000");

    @Override
    public RuleResult evaluate(PaymentOrder order) {
        BigDecimal amount = order.getAmount();

        if (amount.compareTo(VERY_HIGH_AMOUNT_THRESHOLD) >= 0) {
            return new RuleResult(60, "Amount exceeds very high threshold (₹" + VERY_HIGH_AMOUNT_THRESHOLD + ")");
        }
        if (amount.compareTo(HIGH_AMOUNT_THRESHOLD) >= 0) {
            return new RuleResult(30, "Amount exceeds high threshold (₹" + HIGH_AMOUNT_THRESHOLD + ")");
        }
        return RuleResult.clean();
    }
}