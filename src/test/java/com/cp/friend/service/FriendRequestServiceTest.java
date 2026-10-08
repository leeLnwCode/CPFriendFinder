package com.cp.friend.service;

import com.cp.friend.dto.response.FriendRequestResponse;
import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestSentEvent;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Friendship;
import com.cp.friend.model.User;
import com.cp.friend.repository.FriendRequestRepository;
import com.cp.friend.repository.FriendshipRepository;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.event.FriendRequestDeclinedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendRequestServiceTest {

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FriendRequestService friendRequestService;

    @BeforeEach
    void setUp() {
        friendRequestService = new FriendRequestService(
                friendRequestRepository,
                friendshipRepository,
                userRepository,
                userInterestRepository,
                interestRepository,
                new InterestMapper(),
                eventPublisher
        );

        lenient()
                .when(userInterestRepository.findByUserId(any(UUID.class)))
                .thenReturn(List.of());

        lenient()
                .when(friendRequestRepository.save(any(FriendRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User user(
            UUID id,
            String firstname,
            User.Status status
    ) {
        User user = new User();
        setUserId(user, id);

        user.setEmail(
                firstname.toLowerCase() + "@kku.ac.th"
        );
        user.setPasswordHash("encoded-password");
        user.setFirstname(firstname);
        user.setLastname("User");
        user.setImageUrl("avatar.png");
        user.setYear((short) 3);
        user.setDepartment("Computer Science");
        user.setBio("Bio");
        user.setStatus(status);

        return user;
    }

    private FriendRequest request(
            UUID id,
            User sender,
            User receiver,
            FriendRequest.Status status
    ) {
        FriendRequest request = new FriendRequest();
        request.setId(id);
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(status);
        request.setCreatedAt(Instant.now());

        return request;
    }

    @Test
    void send_validUsers_savesPendingAndPublishesEvent() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(friendshipRepository.existsBetween(
                senderId,
                receiverId
        )).thenReturn(false);

        when(friendRequestRepository.existsPendingBetween(
                senderId,
                receiverId
        )).thenReturn(false);

        FriendRequestResponse result =
                friendRequestService.send(
                        senderId,
                        receiverId
                );

        assertNotNull(result);
        assertEquals(receiverId, result.userId());
        assertEquals("Receiver", result.firstname());
        assertEquals(
                FriendRequest.Status.PENDING,
                result.status()
        );

        verify(friendRequestRepository)
                .save(any(FriendRequest.class));

        verify(eventPublisher)
                .publishEvent(
                        any(FriendRequestSentEvent.class)
                );
    }

    @Test
    void send_toSelf_returnsBadRequest() {
        UUID userId = UUID.randomUUID();

        User user = user(
                userId,
                "Same",
                User.Status.ACTIVE
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.send(
                        userId,
                        userId
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        assertEquals(
                "Cannot send a friend request to yourself",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void send_existingFriendship_returnsConflict() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(friendshipRepository.existsBetween(
                senderId,
                receiverId
        )).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.send(
                        senderId,
                        receiverId
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        assertEquals(
                "Users are already friends",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void send_pendingRequest_returnsConflict() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(friendshipRepository.existsBetween(
                senderId,
                receiverId
        )).thenReturn(false);

        when(friendRequestRepository.existsPendingBetween(
                senderId,
                receiverId
        )).thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.send(
                        senderId,
                        receiverId
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        assertEquals(
                "A pending friend request already exists",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void send_receiverUnavailable_returnsNotFound() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.send(
                        senderId,
                        receiverId
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        assertEquals(
                "User not found",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void incoming_returnsPendingRequests() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                UUID.randomUUID(),
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(friendRequestRepository
                .findByReceiverIdAndStatus(
                        receiverId,
                        FriendRequest.Status.PENDING
                ))
                .thenReturn(List.of(request));

        List<FriendRequestResponse> result =
                friendRequestService.incoming(
                        receiverId
                );

        assertEquals(1, result.size());
        assertEquals(
                senderId,
                result.get(0).userId()
        );

        assertEquals(
                FriendRequest.Status.PENDING,
                result.get(0).status()
        );
    }

    @Test
    void outgoing_returnsPendingRequests() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                UUID.randomUUID(),
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(friendRequestRepository
                .findBySenderIdAndStatus(
                        senderId,
                        FriendRequest.Status.PENDING
                ))
                .thenReturn(List.of(request));

        List<FriendRequestResponse> result =
                friendRequestService.outgoing(
                        senderId
                );

        assertEquals(1, result.size());
        assertEquals(
                receiverId,
                result.get(0).userId()
        );

        assertEquals(
                FriendRequest.Status.PENDING,
                result.get(0).status()
        );
    }

    @Test
    void accept_validReceiver_createsFriendshipAndPublishesEvent() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                requestId,
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(friendshipRepository.existsBetween(
                receiverId,
                senderId
        )).thenReturn(false);

        FriendRequestResponse result =
                friendRequestService.accept(
                        receiverId,
                        requestId
                );

        assertEquals(
                FriendRequest.Status.ACCEPTED,
                result.status()
        );

        assertNotNull(
                request.getRespondedAt()
        );

        ArgumentCaptor<Friendship> captor =
                ArgumentCaptor.forClass(
                        Friendship.class
                );

        verify(friendshipRepository)
                .save(captor.capture());

        assertSame(
                receiver,
                captor.getValue().getUser()
        );

        assertSame(
                sender,
                captor.getValue().getFriend()
        );

        verify(eventPublisher)
                .publishEvent(
                        any(
                                FriendRequestAcceptedEvent.class
                        )
                );
    }

    @Test
    void accept_existingFriendship_doesNotDuplicate() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                requestId,
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        when(userRepository.findById(receiverId))
                .thenReturn(Optional.of(receiver));

        when(userRepository.findById(senderId))
                .thenReturn(Optional.of(sender));

        when(friendshipRepository.existsBetween(
                receiverId,
                senderId
        )).thenReturn(true);

        FriendRequestResponse result =
                friendRequestService.accept(
                        receiverId,
                        requestId
                );

        assertEquals(
                FriendRequest.Status.ACCEPTED,
                result.status()
        );

        verify(friendshipRepository, never())
                .save(any());

        verify(friendRequestRepository)
                .save(request);
    }

    @Test
    void accept_nonReceiver_returnsForbidden() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                requestId,
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.accept(
                        anotherUserId,
                        requestId
                )
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                ex.getStatusCode()
        );

        assertEquals(
                "Only the receiver can respond to this request",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void accept_nonPending_returnsConflict() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                requestId,
                sender,
                receiver,
                FriendRequest.Status.ACCEPTED
        );

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.accept(
                        receiverId,
                        requestId
                )
        );

        assertEquals(
                HttpStatus.CONFLICT,
                ex.getStatusCode()
        );

        assertEquals(
                "Friend request is no longer pending",
                ex.getReason()
        );

        verify(friendRequestRepository, never())
                .save(any());
    }

    @Test
    void accept_missingRequest_returnsNotFound() {
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> friendRequestService.accept(
                        receiverId,
                        requestId
                )
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        assertEquals(
                "Friend request not found",
                ex.getReason()
        );
    }

    @Test
    void decline_validReceiver_marksDeclined() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        User sender = user(
                senderId,
                "Sender",
                User.Status.ACTIVE
        );

        User receiver = user(
                receiverId,
                "Receiver",
                User.Status.ACTIVE
        );

        FriendRequest request = request(
                requestId,
                sender,
                receiver,
                FriendRequest.Status.PENDING
        );

        when(friendRequestRepository.findById(requestId))
                .thenReturn(Optional.of(request));

        FriendRequestResponse result =
                friendRequestService.decline(
                        receiverId,
                        requestId
                );

        assertEquals(
                FriendRequest.Status.DECLINED,
                result.status()
        );

        assertNotNull(
                request.getRespondedAt()
        );

        verify(friendRequestRepository)
        .save(request);

        verify(eventPublisher)
                .publishEvent(
                        any(FriendRequestDeclinedEvent.class)
        );
    }

    private void setUserId(
            User user,
            UUID id
    ) {
        try {
            java.lang.reflect.Field field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
