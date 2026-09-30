package com.cp.friend.strategy;

import java.util.Set;

/**
 * Strategy pattern interface for calculating compatibility / match score between users.
 * Follows Open/Closed Principle (OCP) and Liskov Substitution Principle (LSP).
 */
public interface MatchingStrategy {

    /**
     * Calculate matching score (percentage 0.00 to 100.00) between base user interests and target user interests.
     *
     * @param baseInterests   Interests belonging to the current searching user
     * @param targetInterests Interests belonging to candidate user
     * @return Match score between 0.0 and 100.0
     */
    double calculateMatchScore(Set<String> baseInterests, Set<String> targetInterests);

    /**
     * Identifies the strategy implementation name.
     */
    String getStrategyName();
}
