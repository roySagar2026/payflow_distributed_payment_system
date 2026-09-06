package com.payflow.payflow.service;

import com.payflow.payflow.model.*;
import com.payflow.payflow.fraud.FraudRule;
import com.payflow.payflow.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FraudCheckService {

    private final List<FraudRule> fraudRules; // Spring auto-injects ALL beans implementing FraudRule
    private final FraudCheckRepository fraudCheckRepository;

    private static final int REJECTION_THRESHOLD = 55;

    public FraudDecision evaluate(PaymentOrder order) {
        int totalScore = 0;
        StringBuilder triggeredRules = new StringBuilder();

        for (FraudRule rule : fraudRules) {
            FraudRule.RuleResult result = rule.evaluate(order);
            if (result.score() > 0) {
                totalScore += result.score();
                if (!triggeredRules.isEmpty()) triggeredRules.append("; ");
                triggeredRules.append(rule.getClass().getSimpleName())
                        .append(": ").append(result.reason());
            }
        }

        FraudDecision decision = totalScore >= REJECTION_THRESHOLD
                ? FraudDecision.REJECTED
                : FraudDecision.APPROVED;

        FraudCheck check = FraudCheck.builder()
                .paymentOrderId(order.getId())
                .riskScore(totalScore)
                .decision(decision)
                .triggeredRules(triggeredRules.isEmpty() ? null : triggeredRules.toString())
                .build();
        fraudCheckRepository.save(check);

        return decision;
    }
}