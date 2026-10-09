package com.cp.friend.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InterestMatchingStrategyTest {

    private InterestMatchingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new InterestMatchingStrategy();
    }

    @Test
    @DisplayName("Should return 66.67% when 2 out of 3 interests match as specified in project requirements")
    void testPartialMatchFromRequirements() {
        Set<String> baseInterests = Set.of("Java", "AI", "Web");
        Set<String> targetInterests = Set.of("Java", "AI", "Mobile");

        double score = strategy.calculateMatchScore(baseInterests, targetInterests);

        assertEquals(66.67, score, 0.001);
    }

    @Test
    @DisplayName("Should return 100% when all interests match")
    void testFullMatch() {
        Set<String> baseInterests = Set.of("Java", "Spring Boot", "AI");
        Set<String> targetInterests = Set.of("Java", "Spring Boot", "AI", "Docker");

        double score = strategy.calculateMatchScore(baseInterests, targetInterests);

        assertEquals(100.0, score, 0.001);
    }

    @Test
    @DisplayName("Should return 0% when no interests match")
    void testZeroMatch() {
        Set<String> baseInterests = Set.of("Cooking", "Baking");
        Set<String> targetInterests = Set.of("Gaming", "Esports");

        double score = strategy.calculateMatchScore(baseInterests, targetInterests);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    @DisplayName("Should handle case insensitivity and whitespace gracefully")
    void testCaseInsensitiveMatching() {
        Set<String> baseInterests = Set.of("  java  ", "AI");
        Set<String> targetInterests = Set.of("JAVA", "ai ");

        double score = strategy.calculateMatchScore(baseInterests, targetInterests);

        assertEquals(100.0, score, 0.001);
    }

    @Test
    @DisplayName("Should return 0.0 for empty or null collections")
    void testEmptyAndNullScenarios() {
        assertEquals(0.0, strategy.calculateMatchScore(null, Set.of("Java")));
        assertEquals(0.0, strategy.calculateMatchScore(Set.of("Java"), null));
        assertEquals(0.0, strategy.calculateMatchScore(Collections.emptySet(), Set.of("Java")));
        assertEquals(0.0, strategy.calculateMatchScore(Set.of("Java"), Collections.emptySet()));
    }

    @Test
    @DisplayName("Should return strategy identifier")
    void testStrategyName() {
        assertEquals(InterestMatchingStrategy.STRATEGY_NAME, strategy.getStrategyName());
    }
}
