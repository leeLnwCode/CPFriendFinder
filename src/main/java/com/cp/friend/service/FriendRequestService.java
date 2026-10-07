package com.cp.friend.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.response.FriendRequestResponse;
import com.cp.friend.dto.response.InterestResponse;
import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestDeclinedEvent;
import com.cp.friend.event.FriendRequestSentEvent;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.Interest;
import com.cp.friend.model.User;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserInterestRepository userInterestRepository;
    private final InterestRepository interestRepository;
    private final InterestMapper interestMapper;
    // Observer Pattern — publish domain event แทนการเรียก NotificationService ตรง
    // (service นี้ไม่รู้จักระบบแจ้งเตือนเลย — ใครสนใจ event ก็มาฟังเอง)
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Transactional
    public FriendRequestResponse send(UUID senderId, UUID receiverId) {
        User sender = findActiveUser(senderId);
        User receiver = findActiveUser(receiverId);

        if (senderId.equals(receiverId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot send a friend request to yourself");
        }
        if (friendshipRepository.existsBetween(senderId, receiverId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Users are already friends");
        }
        if (friendRequestRepository.existsPendingBetween(senderId, receiverId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A pending friend request already exists");
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequest.Status.PENDING);
        request = friendRequestRepository.save(request);

        eventPublisher.publishEvent(new FriendRequestSentEvent(request));
        return toResponse(request, receiver);
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> incoming(UUID userId) {
        findActiveUser(userId);
        return friendRequestRepository.findByReceiverIdAndStatus(userId, FriendRequest.Status.PENDING)
                .stream()
                .map(request -> toResponse(request, request.getSender()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> outgoing(UUID userId) {
        findActiveUser(userId);
        return friendRequestRepository.findBySenderIdAndStatus(userId, FriendRequest.Status.PENDING)
                .stream()
                .map(request -> toResponse(request, request.getReceiver()))
                .toList();
    }

    @Transactional
    public FriendRequestResponse accept(UUID userId, UUID requestId) {
        FriendRequest request = findRequest(requestId);
        requireReceiver(userId, request);
        requirePending(request);

        User receiver = findActiveUser(userId);
        User sender = findActiveUser(request.getSender().getId());
        if (!friendshipRepository.existsBetween(receiver.getId(), sender.getId())) {
            Friendship friendship = new Friendship();
            friendship.setUser(receiver);
            friendship.setFriend(sender);
            friendshipRepository.save(friendship);
        }

        request.setStatus(FriendRequest.Status.ACCEPTED);
        request.setRespondedAt(Instant.now());
        FriendRequest acceptedRequest = friendRequestRepository.save(request);

        eventPublisher.publishEvent(new FriendRequestAcceptedEvent(acceptedRequest));
        return toResponse(acceptedRequest, sender);
    }

    @Transactional
    public FriendRequestResponse decline(UUID userId, UUID requestId) {
        FriendRequest request = findRequest(requestId);
        requireReceiver(userId, request);
        requirePending(request);

        request.setStatus(FriendRequest.Status.DECLINED);
        request.setRespondedAt(Instant.now());
        FriendRequest declinedRequest = friendRequestRepository.save(request);

        eventPublisher.publishEvent(new FriendRequestDeclinedEvent(declinedRequest));
        return toResponse(declinedRequest, declinedRequest.getSender());
    }

    private User findActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getStatus() != User.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private FriendRequest findRequest(UUID requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Friend request not found"));
    }

    private void requireReceiver(UUID userId, FriendRequest request) {
        if (!request.getReceiver().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the receiver can respond to this request");
        }
    }

    private void requirePending(FriendRequest request) {
        if (request.getStatus() != FriendRequest.Status.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Friend request is no longer pending");
        }
    }

    private FriendRequestResponse toResponse(FriendRequest request, User otherUser) {
        return new FriendRequestResponse(
                request.getId(),
                otherUser.getId(),
                otherUser.getFirstname(),
                otherUser.getLastname(),
                otherUser.getImageUrl(),
                otherUser.getYear(),
                otherUser.getDepartment(),
                otherUser.getBio(),
                interestMapper.toResponseList(loadActiveInterests(otherUser.getId())),
                request.getStatus(),
                request.getCreatedAt()
        );
    }

    // interest ที่ยัง active ของ user — สำหรับแสดงใน popup
    private List<Interest> loadActiveInterests(UUID userId) {
        List<UUID> interestIds = userInterestRepository.findByUserId(userId)
                .stream()
                .map(UserInterest::getInterestId)
                .toList();
        if (interestIds.isEmpty()) {
            return List.of();
        }
        return interestRepository.findByIdInAndIsActiveTrue(new java.util.HashSet<>(interestIds));
    }
}
