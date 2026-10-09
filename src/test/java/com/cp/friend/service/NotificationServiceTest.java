package com.cp.friend.service;

import com.cp.friend.dto.response.NotificationResponse;
import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestSentEvent;
import com.cp.friend.factory.NotificationFactory;
import com.cp.friend.model.*;
import com.cp.friend.repository.NotificationRepository;
import com.cp.friend.repository.RoomMemberRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private RoomMemberRepository roomMemberRepository;

    @Mock
    private NotificationFactory notificationFactory;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                roomMemberRepository,
                notificationFactory,
                messagingTemplate
        );
    }

    private User user(UUID id, String firstname) {
        User user = new User();
        setUserId(user, id);
        user.setFirstname(firstname);
        user.setLastname("User");
        user.setImageUrl("avatar.png");
        return user;
    }

    private Notification notification(User owner) {
        Notification notification = new Notification();
        notification.setUser(owner);
        notification.setType(Notification.Type.FRIEND_REQUEST);
        notification.setTitle("Title");
        notification.setMessage("Message");
        return notification;
    }

    @Test
    void list_allNotifications_mapsResponses() {
        UUID userId = UUID.randomUUID();
        User owner = user(userId, "Owner");

        Notification n = notification(owner);

        when(notificationRepository.findByUserId(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(
                new PageImpl<>(List.of(n))
        );

        assertEquals(
                1,
                notificationService.list(
                        userId,
                        false,
                        0,
                        20
                ).size()
        );

        verify(notificationRepository)
                .findByUserId(
                        eq(userId),
                        any(Pageable.class)
                );
    }

    @Test
    void list_unreadOnly_usesUnreadRepositoryQuery() {
        UUID userId = UUID.randomUUID();

        when(notificationRepository.findUnreadByUserId(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(
                Page.empty()
        );

        notificationService.list(
                userId,
                true,
                0,
                20
        );

        verify(notificationRepository)
                .findUnreadByUserId(
                        eq(userId),
                        any(Pageable.class)
                );

        verify(notificationRepository, never())
                .findByUserId(
                        any(),
                        any()
                );
    }

    @Test
    void list_outOfRangePagination_isClamped() {
        UUID userId = UUID.randomUUID();

        when(notificationRepository.findByUserId(
                eq(userId),
                any(Pageable.class)
        )).thenReturn(
                Page.empty()
        );

        notificationService.list(
                userId,
                false,
                -5,
                1000
        );

        ArgumentCaptor<Pageable> captor =
                ArgumentCaptor.forClass(
                        Pageable.class
                );

        verify(notificationRepository)
                .findByUserId(
                        eq(userId),
                        captor.capture()
                );

        assertEquals(
                0,
                captor.getValue().getPageNumber()
        );

        assertEquals(
                50,
                captor.getValue().getPageSize()
        );
    }

    @Test
    void unreadCount_returnsRepositoryCount() {
        UUID userId = UUID.randomUUID();

        when(notificationRepository
                .countByUserIdAndIsReadFalse(userId))
                .thenReturn(3L);

        assertEquals(
                3L,
                notificationService.unreadCount(userId)
        );
    }

    @Test
    void markAsRead_callsScopedUpdate() {
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();

        notificationService.markAsRead(
                userId,
                notificationId
        );

        verify(notificationRepository)
                .markAsRead(
                        notificationId,
                        userId
                );
    }

    @Test
    void markAllAsRead_callsRepository() {
        UUID userId = UUID.randomUUID();

        notificationService.markAllAsRead(userId);

        verify(notificationRepository)
                .markAllAsRead(userId);
    }

    @Test
    void onFriendRequestSent_savesAndPushesNotification() {
        UUID receiverId = UUID.randomUUID();

        User sender =
                user(UUID.randomUUID(), "Sender");

        User receiver =
                user(receiverId, "Receiver");

        FriendRequest request =
                new FriendRequest();

        request.setSender(sender);
        request.setReceiver(receiver);

        Notification n =
                notification(receiver);

        when(notificationFactory
                .friendRequestReceived(request))
                .thenReturn(n);

        when(notificationRepository.save(n))
                .thenReturn(n);

        notificationService.onFriendRequestSent(
                new FriendRequestSentEvent(request)
        );

        verify(notificationRepository).save(n);

        verify(messagingTemplate)
                .convertAndSend(
                        eq(
                                "/topic/notifications/"
                                        + receiverId
                        ),
                        any(NotificationResponse.class)
                );
    }

    @Test
    void onFriendRequestAccepted_savesAndPushesNotification() {
        UUID senderId = UUID.randomUUID();

        User sender =
                user(senderId, "Sender");

        User receiver =
                user(UUID.randomUUID(), "Receiver");

        FriendRequest request =
                new FriendRequest();

        request.setSender(sender);
        request.setReceiver(receiver);

        Notification n =
                notification(sender);

        when(notificationFactory
                .friendRequestAccepted(request))
                .thenReturn(n);

        when(notificationRepository.save(n))
                .thenReturn(n);

        notificationService.onFriendRequestAccepted(
                new FriendRequestAcceptedEvent(request)
        );

        verify(notificationRepository).save(n);

        verify(messagingTemplate)
                .convertAndSend(
                        eq(
                                "/topic/notifications/"
                                        + senderId
                        ),
                        any(NotificationResponse.class)
                );
    }

    @Test
    void notifyNewMessage_reusesUnreadAndSkipsSender() {
        UUID roomId = UUID.randomUUID();
        UUID senderId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        User sender =
                user(senderId, "Sender");

        User recipient =
                user(recipientId, "Recipient");

        ChatRoom room = new ChatRoom();
        setRoomId(room, roomId);
        room.setRoomName("Study Room");

        Message message = new Message();
        message.setRoom(room);
        message.setSender(sender);

        RoomMember senderMember =
                new RoomMember();

        senderMember.setRoom(room);
        senderMember.setUser(sender);

        RoomMember recipientMember =
                new RoomMember();

        recipientMember.setRoom(room);
        recipientMember.setUser(recipient);

        Notification existing =
                notification(recipient);

        existing.setType(
                Notification.Type.NEW_MESSAGE
        );

        existing.setRoom(room);

        when(roomMemberRepository
                .findActiveMembers(roomId))
                .thenReturn(
                        List.of(
                                senderMember,
                                recipientMember
                        )
                );

        when(notificationRepository
                .findFirstByUserIdAndRoomIdAndTypeAndIsReadFalse(
                        recipientId,
                        roomId,
                        Notification.Type.NEW_MESSAGE
                ))
                .thenReturn(
                        Optional.of(existing)
                );

        when(notificationRepository.save(existing))
                .thenReturn(existing);

        notificationService.notifyNewMessage(
                message
        );

        verify(notificationFactory, never())
                .newMessage(
                        any(),
                        any()
                );

        verify(notificationRepository)
                .save(existing);

        verify(messagingTemplate)
                .convertAndSend(
                        eq(
                                "/topic/notifications/"
                                        + recipientId
                        ),
                        any(NotificationResponse.class)
                );

        assertEquals(
                "New message",
                existing.getTitle()
        );

        assertEquals(
                "Sender User sent a message in Study Room",
                existing.getMessage()
        );
    }

    private void setUserId(
            User user,
            UUID id
    ) {
        try {
            var field =
                    User.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void setRoomId(
            ChatRoom room,
            UUID id
    ) {
        try {
            var field =
                    ChatRoom.class.getDeclaredField("id");

            field.setAccessible(true);
            field.set(room, id);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
