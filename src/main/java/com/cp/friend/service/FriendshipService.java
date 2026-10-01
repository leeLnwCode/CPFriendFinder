package com.cp.friend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.response.FriendRemovalResponse;
import com.cp.friend.dto.response.FriendResponse;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.User;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(UUID userId) {
        ensureActiveUser(userId);
        return friendshipRepository.findAllByMember(userId)
                .stream()
                .map(friendship -> toFriendResponse(userId, friendship))
                .toList();
    }

    @Transactional
    public FriendRemovalResponse unfriend(UUID userId, UUID friendId) {
        ensureActiveUser(userId);
        Friendship friendship = friendshipRepository.findBetween(userId, friendId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found"));

        User friend = friendship.getUser().getId().equals(userId)
                ? friendship.getFriend()
                : friendship.getUser();
        int deleted = friendshipRepository.deleteBetween(userId, friendId);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Friendship not found");
        }

        return new FriendRemovalResponse(true, "Friend removed", friend.getId());
    }

    private void ensureActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private FriendResponse toFriendResponse(UUID userId, Friendship friendship) {
        User friend = friendship.getUser().getId().equals(userId)
                ? friendship.getFriend()
                : friendship.getUser();
        return new FriendResponse(
                friend.getId(),
                friend.getFirstname(),
                friend.getLastname(),
                friend.getImageUrl(),
                friendship.getCreatedAt()
        );
    }
}
