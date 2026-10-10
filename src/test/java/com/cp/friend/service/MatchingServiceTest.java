package com.cp.friend.service;

import com.cp.friend.dto.matching.MatchCandidateResponse;
import com.cp.friend.model.Interest;
import com.cp.friend.model.User;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.service.impl.MatchingServiceImpl;
import com.cp.friend.strategy.InterestMatchingStrategy;
import com.cp.friend.strategy.MatchingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    private MatchingStrategy matchingStrategy;
    private MatchingServiceImpl matchingService;

    private User baseUser;
    private User candidateUser1;
    private User candidateUser2;

    @BeforeEach
    void setUp() {
        matchingStrategy = new InterestMatchingStrategy();
        matchingService = new MatchingServiceImpl(
                userRepository,
                userInterestRepository,
                friendshipRepository,
                friendRequestRepository,
                matchingStrategy
        );

        baseUser = new User();
        baseUser.setEmail("base@kku.ac.th");
        baseUser.setFirstname("Base");
        baseUser.setLastname("User");

        candidateUser1 = new User();
        candidateUser1.setEmail("cand1@kku.ac.th");
        candidateUser1.setFirstname("Candidate");
        candidateUser1.setLastname("One");

        candidateUser2 = new User();
        candidateUser2.setEmail("cand2@kku.ac.th");
        candidateUser2.setFirstname("Candidate");
        candidateUser2.setLastname("Two");
    }

    private UserInterest createUserInterest(UUID userId, String interestName) {
        UserInterest ui = new UserInterest();
        ui.setUserId(userId);
        Interest interest = new Interest();
        interest.setName(interestName);
        ui.setInterest(interest);
        return ui;
    }

    @Test
    @DisplayName("Should successfully recommend friends ranked by matching score descending")
    void testRecommendFriendsRanked() {
        UUID baseId = UUID.randomUUID();
        UUID cand1Id = UUID.randomUUID();
        UUID cand2Id = UUID.randomUUID();

        // Reflection or setter for ID in mock
        setUserId(baseUser, baseId);
        setUserId(candidateUser1, cand1Id);
        setUserId(candidateUser2, cand2Id);

        when(userRepository.findById(baseId)).thenReturn(Optional.of(baseUser));
        when(userRepository.findAllActive()).thenReturn(List.of(baseUser, candidateUser1, candidateUser2));

        // Base user has: Java, AI, Web
        when(userInterestRepository.findByUserIdWithInterest(baseId)).thenReturn(List.of(
                createUserInterest(baseId, "Java"),
                createUserInterest(baseId, "AI"),
                createUserInterest(baseId, "Web")
        ));

        // Candidate 1 has: Java, AI, Mobile -> 2/3 = 66.67%
        when(userInterestRepository.findByUserIdsWithInterest(any())).thenReturn(List.of(
                createUserInterest(cand1Id, "Java"),
                createUserInterest(cand1Id, "AI"),
                createUserInterest(cand1Id, "Mobile"),
                createUserInterest(cand2Id, "Java"),
                createUserInterest(cand2Id, "AI"),
                createUserInterest(cand2Id, "Web")
        ));


        List<MatchCandidateResponse> results = matchingService.recommendFriends(baseId, 10);

        assertNotNull(results);
        assertEquals(2, results.size());

        // First candidate should be cand2 with 100.0%
        assertEquals(cand2Id, results.get(0).getUserId());
        assertEquals(100.0, results.get(0).getMatchScore());

        // Second candidate should be cand1 with 66.67%
        assertEquals(cand1Id, results.get(1).getUserId());
        assertEquals(66.67, results.get(1).getMatchScore(), 0.01);
    }

    @Test
    @DisplayName("Should exclude already added friends and pending requests from recommendations")
    void testExcludeExistingFriendsAndPending() {
        UUID baseId = UUID.randomUUID();
        UUID friendId = UUID.randomUUID();
        UUID pendingId = UUID.randomUUID();

        setUserId(baseUser, baseId);
        setUserId(candidateUser1, friendId);
        setUserId(candidateUser2, pendingId);

        when(userRepository.findById(baseId)).thenReturn(Optional.of(baseUser));
        when(userRepository.findAllActive()).thenReturn(List.of(baseUser, candidateUser1, candidateUser2));
        when(userInterestRepository.findByUserIdWithInterest(baseId)).thenReturn(Collections.emptyList());

        com.cp.friend.model.Friendship friendship = new com.cp.friend.model.Friendship();
        friendship.setUser(baseUser);
        friendship.setFriend(candidateUser1);
        when(friendshipRepository.findAllByMember(baseId)).thenReturn(List.of(friendship));
        com.cp.friend.model.FriendRequest pending = new com.cp.friend.model.FriendRequest();
        pending.setSender(baseUser);
        pending.setReceiver(candidateUser2);
        when(friendRequestRepository.findBySenderIdAndStatus(baseId, com.cp.friend.model.FriendRequest.Status.PENDING))
                .thenReturn(List.of(pending));

        List<MatchCandidateResponse> results = matchingService.recommendFriends(baseId, 10);

        assertTrue(results.isEmpty(), "Candidates who are already friends or pending should be excluded");
    }

    @Test
    @DisplayName("Should correctly calculate compatibility between two users")
    void testCalculateCompatibility() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        when(userInterestRepository.findByUserIdWithInterest(user1)).thenReturn(List.of(
                createUserInterest(user1, "Python"),
                createUserInterest(user1, "DevOps")
        ));

        when(userInterestRepository.findByUserIdWithInterest(user2)).thenReturn(List.of(
                createUserInterest(user2, "Python")
        ));

        double score = matchingService.calculateCompatibility(user1, user2);
        // 1/2 = 50.0%
        assertEquals(50.0, score, 0.01);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException if user does not exist")
    void testUserNotFoundThrows() {
        UUID nonExistent = UUID.randomUUID();
        when(userRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> matchingService.recommendFriends(nonExistent, 5));
    }

    private void setUserId(User user, UUID id) {
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
