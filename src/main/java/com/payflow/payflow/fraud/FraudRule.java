package com.payflow.payflow.fraud;

import com.payflow.payflow.model.PaymentOrder;

public interface FraudRule {

    /**
     * Returns a risk score contribution (0-100) for this rule, and a human-readable reason
     * if the rule was triggered (non-zero score). Return 0 + null reason if the rule doesn't apply.
     */
    RuleResult evaluate(PaymentOrder order);

    record RuleResult(int score, String reason) {
        public static RuleResult clean() {
            return new RuleResult(0, null);
        }
    }
}