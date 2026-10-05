package com.cp.friend.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cp.friend.dto.response.NotificationResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.FriendRequest;
import com.cp.friend.model.Notification;
import com.cp.friend.model.RoomMember;
import com.cp.friend.model.User;
import com.cp.friend.repository.NotificationRepository;
import com.cp.friend.repository.RoomMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // =========================================================
    // สร้าง notification จาก event ต่างๆ — บันทึกลง DB แล้ว push แบบ real-time
    // =========================================================

    @Transactional
    public void notifyFriendRequestReceived(FriendRequest request) {
        create(
                request.getReceiver(),
                request.getSender(),
                Notification.Type.FRIEND_REQUEST,
                "New friend request",
                "%s sent you a friend request".formatted(
                        fullName(request.getSender())),
                request,
                null
        );
    }

    @Transactional
    public void notifyFriendRequestAccepted(FriendRequest request) {
        create(
                request.getSender(),
                request.getReceiver(),
                Notification.Type.FRIEND_REQUEST,
                "Friend request accepted",
                "%s accepted your friend request".formatted(
                        fullName(request.getReceiver())),
                request,
                null
        );
    }

    // แจ้งเตือนข้อความใหม่ให้สมาชิกทุกคนในห้อง (ยกเว้นผู้ส่ง)
    // ถ้ามี notification ที่ยังไม่อ่านของห้องนี้อยู่แล้ว จะอัปเดตรายการเดิม ไม่สร้างใหม่ — กันสแปมเวลาคุยกันรัวๆ
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
                    .orElseGet(() -> {
                        Notification n = new Notification();
                        n.setUser(recipient);
                        n.setActor(sender);
                        n.setType(Notification.Type.NEW_MESSAGE);
                        n.setRoom(room);
                        return n;
                    });

            notification.setTitle("New message");
            notification.setMessage("%s sent a message in %s".formatted(
                    fullName(sender), room.getRoomName()));
            NotificationResponse response = toResponse(notificationRepository.save(notification));

            messagingTemplate.convertAndSendToUser(
                    recipient.getId().toString(), "/queue/notifications", response);
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

    private void create(User user, User actor, Notification.Type type,
                        String title, String message,
                        FriendRequest friendRequest, ChatRoom room) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setActor(actor);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setFriendRequest(friendRequest);
        notification.setRoom(room);
        NotificationResponse response = toResponse(notificationRepository.save(notification));

        // push แบบ real-time — client subscribe /user/queue/notifications
        // (user ที่ offline จะไม่ได้ push แต่ข้อมูลอยู่ใน DB รอเปิดหน้าแล้วโหลด)
        messagingTemplate.convertAndSendToUser(
                user.getId().toString(), "/queue/notifications", response);
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
