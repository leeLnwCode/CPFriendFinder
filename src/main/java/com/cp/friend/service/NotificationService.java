package com.cp.friend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cp.friend.dto.response.NotificationResponse;
import com.cp.friend.event.FriendRequestAcceptedEvent;
import com.cp.friend.event.FriendRequestSentEvent;
import com.cp.friend.factory.NotificationFactory;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Notification;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.NotificationRepository;
import com.cp.friend.repository.RoomMemberRepository;

import lombok.RequiredArgsConstructor;

// Observer Pattern — NotificationService เป็น Listener ของ domain events
// (FriendRequestService publish event แล้วไม่รู้จัก service นี้เลย — decouple ผ่าน ApplicationEvent)
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final NotificationFactory notificationFactory;
    private final SimpMessagingTemplate messagingTemplate;

    // =========================================================
    // Observers — รับ domain events แล้วสร้าง notification ผ่าน Factory
    // =========================================================

    @EventListener
    @Transactional
    public void onFriendRequestSent(FriendRequestSentEvent event) {
        FriendRequest request = event.request();
        saveAndPush(notificationFactory.friendRequestReceived(request));
    }

    @EventListener
    @Transactional
    public void onFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        FriendRequest request = event.request();
        saveAndPush(notificationFactory.friendRequestAccepted(request));
    }

    // แจ้งเตือนข้อความใหม่ให้สมาชิกทุกคนในห้อง (ยกเว้นผู้ส่ง)
    // ถ้ามี notification ที่ยังไม่อ่านของห้องนี้อยู่แล้ว จะอัปเดตรายการเดิม ไม่สร้างใหม่ — กันสแปม
    @Transactional
    public void notifyNewMessage(com.cp.friend.model.Message message) {
        ChatRoom room = message.getRoom();
        User sender = message.getSender();

        for (RoomMember member : roomMemberRepository.findActiveMembers(room.getId())) {
            User recipient = member.getUser();
            if (recipient.getId().equals(sender.getId())) {
                continue;
            }

            Notification notification = notificationRepository
                    .findFirstByUserIdAndRoomIdAndTypeAndIsReadFalse(
                            recipient.getId(), room.getId(), Notification.Type.NEW_MESSAGE)
                    .orElseGet(() -> notificationFactory.newMessage(message, recipient));

            notification.setTitle("New message");
            notification.setMessage("%s sent a message in %s".formatted(
                    fullName(sender), room.getRoomName()));
            saveAndPush(notification);
        }
    }

    // =========================================================
    // อ่าน / จัดการ notification ของตัวเอง
    // =========================================================

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID userId, boolean unreadOnly, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return (unreadOnly
                ? notificationRepository.findUnreadByUserId(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable))
                .map(NotificationService::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        // markAsRead ตรวจว่าเป็นของ user นี้จริงใน where แล้ว — ถ้าไม่ใช่ก็ไม่มีอะไรเปลี่ยน
        notificationRepository.markAsRead(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // =========================================================
    // helpers
    // =========================================================

    private void saveAndPush(Notification notification) {
        NotificationResponse response = toResponse(notificationRepository.save(notification));

        // push แบบ real-time — client subscribe /topic/notifications/{userId}
        // (broadcast ต่อ user แทน /user/queue — convertAndSendToUser ยังส่งไม่ได้ใน Spring Boot 4.1.1)
        messagingTemplate.convertAndSend(
                "/topic/notifications/" + notification.getUser().getId(), response);
    }

    private static String fullName(User user) {
        return "%s %s".formatted(user.getFirstname(), user.getLastname());
    }

    private static NotificationResponse toResponse(Notification notification) {
        User actor = notification.getActor();
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getFriendRequest() != null
                        ? notification.getFriendRequest().getId() : null,
                notification.getRoom() != null
                        ? notification.getRoom().getId() : null,
                actor != null
                        ? new NotificationResponse.ActorDto(
                                actor.getId(),
                                actor.getFirstname(),
                                actor.getLastname(),
                                actor.getImageUrl())
                        : null
        );
    }
}
