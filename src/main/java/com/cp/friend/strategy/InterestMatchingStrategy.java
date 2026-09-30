package com.cp.friend.strategy;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Concrete Strategy that computes match score based on common interests.
 * Example from requirements:
 * Base User: [Java, AI, Web]
 * Candidate:  [Java, AI, Mobile]
 * Shared: 2/3 -> 66.67%
 */
@Component("interestMatchingStrategy")
public class InterestMatchingStrategy implements MatchingStrategy {

    public static final String STRATEGY_NAME = "INTEREST_BASED";

    @Override
    public double calculateMatchScore(Set<String> baseInterests, Set<String> targetInterests) {
        if (baseInterests == null || baseInterests.isEmpty() || targetInterests == null || targetInterests.isEmpty()) {
            return 0.0;
        }

        // Normalize case for robust matching
        Set<String> normalizedBase = baseInterests.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (normalizedBase.isEmpty()) {
            return 0.0;
        }

        Set<String> normalizedTarget = targetInterests.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase())
                .collect(Collectors.toSet());

        long matchCount = normalizedBase.stream()
                .filter(normalizedTarget::contains)
                .count();

        double rawScore = ((double) matchCount / (double) normalizedBase.size()) * 100.0;

        return BigDecimal.valueOf(rawScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
}
