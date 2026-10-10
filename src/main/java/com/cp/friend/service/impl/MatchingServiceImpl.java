package com.cp.friend.service.impl;

import com.cp.friend.dto.matching.MatchCandidateResponse;
import com.cp.friend.model.User;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.service.MatchingService;
import com.cp.friend.strategy.MatchingStrategy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of MatchingService following SOLID principles:
 * - Single Responsibility Principle (SRP): Focuses solely on matching and recommendation logic.
 * - Open/Closed Principle (OCP): Matching algorithm is delegated to MatchingStrategy without altering service code.
 * - Dependency Inversion Principle (DIP): Injected via constructor dependencies and interfaces.
 */
@Service
@Transactional(readOnly = true)
public class MatchingServiceImpl implements MatchingService {

    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private MatchingStrategy matchingStrategy;

    public MatchingServiceImpl(
            UserRepository userRepository,
            UserInterestRepository userInterestRepository,
            FriendshipRepository friendshipRepository,
            FriendRequestRepository friendRequestRepository,
            @Qualifier("interestMatchingStrategy") MatchingStrategy matchingStrategy
    ) {
        this.userRepository = userRepository;
        this.userInterestRepository = userInterestRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.matchingStrategy = matchingStrategy;
    }

    @Override
    public synchronized void setMatchingStrategy(MatchingStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("MatchingStrategy cannot be null");
        }
        this.matchingStrategy = strategy;
    }

    @Override
    public List<MatchCandidateResponse> recommendFriends(UUID userId, int limit) {
        User baseUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Set<String> baseInterests = getUserInterests(userId);

        List<User> activeUsers = userRepository.findAllActive();

        // Load exclusions and interests in batches instead of querying for every candidate.
        Set<UUID> excluded = new HashSet<>();
        excluded.add(baseUser.getId());
        friendshipRepository.findAllByMember(userId).forEach(f -> {
            excluded.add(f.getUser().getId()); excluded.add(f.getFriend().getId());
        });
        friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequest.Status.PENDING)
                .forEach(fr -> excluded.add(fr.getReceiver().getId()));
        friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequest.Status.PENDING)
                .forEach(fr -> excluded.add(fr.getSender().getId()));
        List<UUID> eligibleIds = activeUsers.stream().map(User::getId)
                .filter(id -> !excluded.contains(id)).toList();
        Map<UUID, Set<String>> interestsByUser = new HashMap<>();
        if (!eligibleIds.isEmpty()) {
            for (UserInterest interest : userInterestRepository.findByUserIdsWithInterest(eligibleIds)) {
                if (interest.getInterest() != null && interest.getInterest().getName() != null)
                    interestsByUser.computeIfAbsent(interest.getUserId(), id -> new HashSet<>())
                            .add(interest.getInterest().getName());
            }
        }

        List<MatchCandidateResponse> candidates = activeUsers.stream()
                // Exclude self
                .filter(u -> !excluded.contains(u.getId()))
                // Exclude existing friends
                // Exclude pending friend requests (in either direction)
                .map(candidate -> {
                    Set<String> candidateInterests = interestsByUser.getOrDefault(candidate.getId(), Collections.emptySet());
                    double score = matchingStrategy.calculateMatchScore(baseInterests, candidateInterests);

                    Set<String> shared = baseInterests.stream()
                            .filter(b -> candidateInterests.stream().anyMatch(c -> c.equalsIgnoreCase(b)))
                            .collect(Collectors.toSet());

                    return MatchCandidateResponse.builder()
                            .userId(candidate.getId())
                            .firstname(candidate.getFirstname())
                            .lastname(candidate.getLastname())
                            .imageUrl(candidate.getImageUrl())
                            .galleryPhotos(candidate.getGalleryPhotos())
                            .bio(candidate.getBio())
                            .department(candidate.getDepartment())
                            .year(candidate.getYear())
                            .matchScore(score)
                            .sharedInterests(shared)
                            .build();
                })
                .sorted(Comparator.comparingDouble(MatchCandidateResponse::getMatchScore).reversed())
                .limit(limit > 0 ? limit : 10)
                .collect(Collectors.toList());

        return candidates;
    }

    @Override
    public double calculateCompatibility(UUID userId1, UUID userId2) {
        Set<String> interests1 = getUserInterests(userId1);
        Set<String> interests2 = getUserInterests(userId2);
        return matchingStrategy.calculateMatchScore(interests1, interests2);
    }

    private Set<String> getUserInterests(UUID userId) {
        List<UserInterest> userInterests = userInterestRepository.findByUserIdWithInterest(userId);
        if (userInterests == null || userInterests.isEmpty()) {
            return Collections.emptySet();
        }
        return userInterests.stream()
                .filter(ui -> ui.getInterest() != null && ui.getInterest().getName() != null)
                .map(ui -> ui.getInterest().getName())
                .collect(Collectors.toSet());
    }
}
