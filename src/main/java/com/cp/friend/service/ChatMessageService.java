package com.cp.friend.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.config.S3Config;
import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.response.CallSignalResponse;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.Message;
import com.cp.friend.model.RoomMember;
import com.cp.friend.repository.ChatRoomRepository;
import com.cp.friend.repository.MessageRepository;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.tools.StorageTool;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_IMAGE_BASE64_LENGTH = 4_000_000; // ~3MB binary

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final NotificationService notificationService;
    private final StorageTool storageTool;
    private final S3Config s3Config;
    private final SimpMessagingTemplate messagingTemplate;

    // =========================================================
    // โหลดข้อความ (เฉพาะสมาชิกห้อง) — โหลดแล้วถือว่าอ่านแล้ว (mark last_read_at)
    // =========================================================

    @Transactional
    public List<ChatMessageResponse> getMessages(UUID userId, UUID roomId, Instant before, int limit) {
        requireActiveMember(roomId, userId);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        // ห้องกลุ่มเก็บข้อความแบบ real-time เท่านั้น — ไม่มีประวัติให้ย้อนดู
        if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_PAGE_SIZE));

        List<Message> messages = before == null
                ? messageRepository.findLatestByRoomId(roomId, pageable)
                : messageRepository.findByRoomIdBefore(roomId, before, pageable);

        // จัดเรียงเก่า → ใหม่ เพื่อให้ client แสดงต่อท้ายได้เลย
        List<ChatMessageResponse> response = messages.reversed().stream()
                .map(ChatMessageService::toResponse)
                .toList();

        markRead(roomId, userId);
        return response;
    }

    // จำนวนข้อความที่ยังไม่อ่านในห้อง (badge)
    @Transactional(readOnly = true)
    public long unreadCount(UUID userId, UUID roomId) {
        requireActiveMember(roomId, userId);
        return countUnread(userId, roomMemberRepository.findActiveMember(roomId, userId).orElseThrow());
    }

    // =========================================================
    // ส่งข้อความ — ใช้ทั้งจาก WebSocket และ REST
    // persist → broadcast ให้ทุกคนในห้อง → แจ้งเตือนสมาชิกที่ไม่ได้เปิดหน้า
    // =========================================================

    @Transactional
    public ChatMessageResponse send(UUID userId, UUID roomId, ChatMessagePayload payload) {
        RoomMember senderMember = requireActiveMember(roomId, userId);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        String content = payload.getContent() == null ? "" : payload.getContent().trim();
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
        Message.MessageType messageType = parseMessageType(payload.getMessageType());

        // รูป: รับ base64 แล้วอัปโหลดขึ้น S3 — ทั้งห้องกลุ่มและ direct ส่ง URL ต่อให้ผู้รับเหมือนกัน
        if (messageType == Message.MessageType.IMAGE) {
            if (content.length() > MAX_IMAGE_BASE64_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is too large (max ~3MB)");
            }
            String key = storageTool.uploadBase64(content);
            content = s3Config.getEndpoint().concat("/storage/").concat(key);
        }

        // ห้องกลุ่ม: broadcast อย่างเดียว — ไม่บันทึก DB, ไม่มี notification, ไม่มี unread
        // (ข้อความมีชีวิตอยู่เฉพาะตอนที่คนในห้องออนไลน์อยู่ด้วยกัน)
        if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
            ChatMessageResponse response = new ChatMessageResponse(
                    null,
                    roomId,
                    senderMember.getUser().getId(),
                    senderMember.getUser().getFirstname(),
                    senderMember.getUser().getLastname(),
                    senderMember.getUser().getImageUrl(),
                    content,
                    messageType,
                    Instant.now()
            );
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, response);
            return response;
        }

        // ห้อง DIRECT: บันทึกลง DB (มีประวัติ ย้อนดูได้) + แจ้งเตือนสมาชิกคนอื่น
        Message message = new Message();
        message.setRoom(room);
        message.setSender(senderMember.getUser());
        message.setContent(content);
        message.setMessageType(messageType);
        message = messageRepository.save(message);

        // ผู้ส่งถือว่าอ่านถึงข้อความตัวเองแล้ว
        senderMember.setLastReadAt(Instant.now());

        ChatMessageResponse response = toResponse(message);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, response);
        notificationService.notifyNewMessage(message);
        return response;
    }

    // =========================================================
    // WebRTC signaling — backend เป็นแค่สายส่ง ไม่เก็บสถานะการคอลใดๆ
    //
    // ฝั่ง client:
    // - ส่ง: /app/rooms/{roomId}/call  พร้อม {type, targetUserId?, payload}
    // - รับ broadcast (JOIN/LEAVE): /topic/rooms/{roomId}/call
    // - รับส่งตรง (OFFER/ANSWER/ICE): /user/queue/call
    // =========================================================

    public void relayCallSignal(UUID userId, UUID roomId, CallSignalRequest signal) {
        requireActiveMember(roomId, userId);

        String type = signal.getType() == null ? "" : signal.getType().trim().toUpperCase(Locale.ROOT);
        UUID fromUserId = userId;

        switch (type) {
            case "JOIN", "LEAVE" -> messagingTemplate.convertAndSend(
                    "/topic/rooms/" + roomId + "/call",
                    new CallSignalResponse(type, fromUserId, null));

            case "OFFER", "ANSWER", "ICE" -> {
                if (signal.getTargetUserId() == null) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, type + " requires targetUserId");
                }
                messagingTemplate.convertAndSendToUser(
                        signal.getTargetUserId().toString(),
                        "/queue/call",
                        new CallSignalResponse(type, fromUserId, signal.getPayload()));
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid signal type, use JOIN, LEAVE, OFFER, ANSWER or ICE");
        }
    }

    // =========================================================
    // ลบข้อความตัวเอง (soft delete)
    // =========================================================

    @Transactional
    public ChatMessageResponse delete(UUID userId, UUID roomId, UUID messageId) {
        requireActiveMember(roomId, userId);

        Message message = messageRepository.findById(messageId)
                .filter(m -> m.getRoom().getId().equals(roomId) && m.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own messages");
        }

        message.setDeletedAt(Instant.now());
        return toResponse(messageRepository.save(message));
    }

    // =========================================================
    // helpers
    // =========================================================

    private RoomMember requireActiveMember(UUID roomId, UUID userId) {
        return roomMemberRepository.findActiveMember(roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not a member of this room"));
    }

    private void markRead(UUID roomId, UUID userId) {
        roomMemberRepository.findActiveMember(roomId, userId)
                .ifPresent(member -> member.setLastReadAt(Instant.now()));
    }

    private long countUnread(UUID userId, RoomMember member) {
        if (member.getLastReadAt() == null) {
            return messageRepository.countByRoomIdAndDeletedAtIsNullAndSenderIdNot(
                    member.getRoom().getId(), userId);
        }
        return messageRepository.countByRoomIdAndDeletedAtIsNullAndSenderIdNotAndCreatedAtAfter(
                member.getRoom().getId(), userId, member.getLastReadAt());
    }

    private Message.MessageType parseMessageType(String raw) {
        if (raw == null || raw.isBlank()) {
            return Message.MessageType.TEXT;
        }
        try {
            return Message.MessageType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid messageType");
        }
    }

    private static ChatMessageResponse toResponse(Message message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getRoom().getId(),
                message.getSender().getId(),
                message.getSender().getFirstname(),
                message.getSender().getLastname(),
                message.getSender().getImageUrl(),
                message.getContent(),
                message.getMessageType(),
                message.getCreatedAt()
        );
    }
}
