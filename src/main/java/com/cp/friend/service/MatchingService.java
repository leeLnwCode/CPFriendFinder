package com.cp.friend.service;

import com.cp.friend.dto.matching.MatchCandidateResponse;
import com.cp.friend.strategy.MatchingStrategy;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for friend matching and recommendations.
 */
public interface MatchingService {

    /**
     * Recommends candidate friends for a given user, ranked by matching score descending.
     *
     * @param userId The user ID seeking recommendations
     * @param limit  Maximum candidates to return
     * @return List of ranked candidates with match score
     */
    List<MatchCandidateResponse> recommendFriends(UUID userId, int limit);

    /**
     * Calculates compatibility score between two specific users.
     *
     * @param userId1 First user ID
     * @param userId2 Second user ID
     * @return Compatibility score (0.0 to 100.0)
     */
    double calculateCompatibility(UUID userId1, UUID userId2);

    /**
     * Dynamically swaps the matching strategy at runtime (GoF Strategy Pattern).
     *
     * @param strategy The new matching strategy to use
     */
    void setMatchingStrategy(MatchingStrategy strategy);
}
