package com.cp.friend.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cp.friend.dto.response.UserDiscoveryResponse;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.User;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDiscoveryService {

    private final UserInterestRepository userInterestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserDiscoveryResponse> discover(UUID userId) {
        List<UserInterestRepository.CommonInterestCount> matches =
                userInterestRepository.findUsersWithCommonInterests(userId);
        if (matches.isEmpty()) {
            return List.of();
        }

        Set<UUID> excludedUserIds = findExcludedUserIds(userId);
        Set<UUID> candidateIds = new HashSet<>();
        for (UserInterestRepository.CommonInterestCount match : matches) {
            if (!excludedUserIds.contains(match.getUserId())) {
                candidateIds.add(match.getUserId());
            }
        }
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, User> activeUsersById = new HashMap<>();
        for (User candidate : userRepository.findAllById(candidateIds)) {
            if (candidate.getStatus() == User.Status.ACTIVE) {
                activeUsersById.put(candidate.getId(), candidate);
            }
        }

        List<UserDiscoveryResponse> recommendations = new ArrayList<>();
        for (UserInterestRepository.CommonInterestCount match : matches) {
            User candidate = activeUsersById.get(match.getUserId());
            if (candidate != null) {
                recommendations.add(new UserDiscoveryResponse(
                        candidate.getId(),
                        candidate.getFirstname(),
                        candidate.getLastname(),
                        candidate.getImageUrl(),
                        match.getCommonCount()
                ));
            }
        }
        return recommendations;
    }

    private Set<UUID> findExcludedUserIds(UUID userId) {
        Set<UUID> excludedUserIds = new HashSet<>();

        for (Friendship friendship : friendshipRepository.findAllByMember(userId)) {
            UUID otherUserId = friendship.getUser().getId().equals(userId)
                    ? friendship.getFriend().getId()
                    : friendship.getUser().getId();
            excludedUserIds.add(otherUserId);
        }

        for (FriendRequest request : friendRequestRepository.findBySenderIdAndStatus(
                userId,
                FriendRequest.Status.PENDING
        )) {
            excludedUserIds.add(request.getReceiver().getId());
        }

        for (FriendRequest request : friendRequestRepository.findByReceiverIdAndStatus(
                userId,
                FriendRequest.Status.PENDING
        )) {
            excludedUserIds.add(request.getSender().getId());
        }

        excludedUserIds.add(userId);
        return excludedUserIds;
    }
}
