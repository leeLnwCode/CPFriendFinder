package com.cp.friend.controller;

import com.cp.friend.dto.matching.MatchCandidateResponse;
import com.cp.friend.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for friend matching and recommendations.
 * Uses constructor injection to satisfy SOLID Dependency Inversion Principle.
 */
@RestController
@RequestMapping("/api/v1/matching")
@Tag(name = "Matching", description = "Endpoints for friend discovery and recommendation using Strategy Pattern")
public class MatchingController {

    private final MatchingService matchingService;

    public MatchingController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @GetMapping("/recommendations/{userId}")
    @Operation(summary = "Get friend recommendations for a user based on shared interests")
    public ResponseEntity<List<MatchCandidateResponse>> getRecommendations(
            @Parameter(description = "UUID of the user seeking recommendations")
            @PathVariable UUID userId,
            @Parameter(description = "Maximum candidates to return (default 10)")
            @RequestParam(defaultValue = "10") int limit
    ) {
        List<MatchCandidateResponse> candidates = matchingService.recommendFriends(userId, limit);
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/compatibility")
    @Operation(summary = "Calculate compatibility score between two users")
    public ResponseEntity<Map<String, Object>> getCompatibility(
            @Parameter(description = "First user UUID")
            @RequestParam UUID userId1,
            @Parameter(description = "Second user UUID")
            @RequestParam UUID userId2
    ) {
        double score = matchingService.calculateCompatibility(userId1, userId2);
        return ResponseEntity.ok(Map.of(
                "userId1", userId1,
                "userId2", userId2,
                "compatibilityScore", score
        ));
    }
}
