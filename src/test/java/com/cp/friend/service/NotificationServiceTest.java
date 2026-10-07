package com.cp.friend.service;

import com.cp.friend.dto.response.NotificationResponse;
import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestDeclinedEvent;
import com.cp.friend.event.FriendRequestSentEvent;
import com.cp.friend.factory.NotificationFactory;
import com.cp.friend.model.*;
import com.cp.friend.repository.NotificationRepository;
import com.cp.friend.repository.RoomMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private User sender;
    private User receiver;
    private FriendRequest friendRequest;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                roomMemberRepository,
                notificationFactory,
                messagingTemplate
        );

        sender = new User();
        setEntityId(sender, UUID.randomUUID());
        sender.setFirstname("Somchai");
        sender.setLastname("Jaidee");

        receiver = new User();
        setEntityId(receiver, UUID.randomUUID());
        receiver.setFirstname("Somsri");
        receiver.setLastname("Rukdee");

        friendRequest = new FriendRequest();
        setEntityId(friendRequest, UUID.randomUUID());
        friendRequest.setSender(sender);
        friendRequest.setReceiver(receiver);
        friendRequest.setStatus(FriendRequest.Status.PENDING);
    }

    @Test
    @DisplayName("Observer: Should handle FriendRequestSentEvent and push notification to receiver")
    void testOnFriendRequestSent() {
        Notification notification = new Notification();
        setEntityId(notification, UUID.randomUUID());
        notification.setUser(receiver);
        notification.setActor(sender);
        notification.setType(Notification.Type.FRIEND_REQUEST);
        notification.setTitle("New friend request");
        notification.setMessage("Somchai Jaidee sent you a friend request");

        when(notificationFactory.friendRequestReceived(friendRequest)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.onFriendRequestSent(new FriendRequestSentEvent(friendRequest));

        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + receiver.getId()), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Observer: Should handle FriendRequestAcceptedEvent and push notification to sender")
    void testOnFriendRequestAccepted() {
        friendRequest.setStatus(FriendRequest.Status.ACCEPTED);

        Notification notification = new Notification();
        setEntityId(notification, UUID.randomUUID());
        notification.setUser(sender);
        notification.setActor(receiver);
        notification.setType(Notification.Type.FRIEND_REQUEST);
        notification.setTitle("Friend request accepted");
        notification.setMessage("Somsri Rukdee accepted your friend request");

        when(notificationFactory.friendRequestAccepted(friendRequest)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.onFriendRequestAccepted(new FriendRequestAcceptedEvent(friendRequest));

        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + sender.getId()), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Observer: Should handle FriendRequestDeclinedEvent and push notification to sender")
    void testOnFriendRequestDeclined() {
        friendRequest.setStatus(FriendRequest.Status.DECLINED);

        Notification notification = new Notification();
        setEntityId(notification, UUID.randomUUID());
        notification.setUser(sender);
        notification.setActor(receiver);
        notification.setType(Notification.Type.FRIEND_REQUEST);
        notification.setTitle("Friend request declined");
        notification.setMessage("Somsri Rukdee declined your friend request");

        when(notificationFactory.friendRequestDeclined(friendRequest)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);

        notificationService.onFriendRequestDeclined(new FriendRequestDeclinedEvent(friendRequest));

        verify(notificationRepository).save(notification);
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + sender.getId()), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Should notify room members excluding the message sender")
    void testNotifyNewMessageExcludesSender() {
        ChatRoom room = new ChatRoom();
        setEntityId(room, UUID.randomUUID());
        room.setRoomName("Study Room");

        Message message = new Message();
        setEntityId(message, UUID.randomUUID());
        message.setRoom(room);
        message.setSender(sender);
        message.setContent("Hello team");

        RoomMember senderMember = new RoomMember();
        senderMember.setUser(sender);

        RoomMember recipientMember = new RoomMember();
        recipientMember.setUser(receiver);

        when(roomMemberRepository.findActiveMembers(room.getId())).thenReturn(List.of(senderMember, recipientMember));

        Notification newNotification = new Notification();
        setEntityId(newNotification, UUID.randomUUID());
        newNotification.setUser(receiver);
        newNotification.setActor(sender);
        newNotification.setType(Notification.Type.NEW_MESSAGE);

        when(notificationRepository.findFirstByUserIdAndRoomIdAndTypeAndIsReadFalse(receiver.getId(), room.getId(), Notification.Type.NEW_MESSAGE))
                .thenReturn(java.util.Optional.empty());
        when(notificationFactory.newMessage(message, receiver)).thenReturn(newNotification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(newNotification);

        notificationService.notifyNewMessage(message);

        // Sender should NOT receive notification, only receiver should
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/notifications/" + sender.getId()), any(NotificationResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/" + receiver.getId()), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Should return list of notifications for user")
    void testListNotifications() {
        UUID userId = receiver.getId();
        Notification n = new Notification();
        setEntityId(n, UUID.randomUUID());
        n.setUser(receiver);
        n.setType(Notification.Type.SYSTEM);
        n.setTitle("Welcome");
        n.setMessage("Welcome to CP Friend Finder");

        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(n)));

        List<NotificationResponse> result = notificationService.list(userId, false, 0, 10);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Welcome", result.get(0).title());
    }

    @Test
    @DisplayName("Should query unread notification count")
    void testUnreadCount() {
        UUID userId = receiver.getId();
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

        long count = notificationService.unreadCount(userId);

        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(userId);
    }

    @Test
    @DisplayName("Should mark notification as read")
    void testMarkAsRead() {
        UUID userId = receiver.getId();
        UUID notifId = UUID.randomUUID();

        notificationService.markAsRead(userId, notifId);

        verify(notificationRepository).markAsRead(notifId, userId);
    }

    @Test
    @DisplayName("Should mark all notifications as read")
    void testMarkAllAsRead() {
        UUID userId = receiver.getId();

        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(userId);
    }

    private void setEntityId(Object target, UUID id) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
