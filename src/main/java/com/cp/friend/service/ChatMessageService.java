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

import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.Message;
import com.cp.friend.model.RoomMember;
import com.cp.friend.repository.ChatRoomRepository;
import com.cp.friend.repository.MessageRepository;
import com.cp.friend.repository.RoomMemberRepository;
import com.cp.friend.service.strategy.MessageContentStrategyResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final com.cp.friend.port.MessageNotifications notificationService;
    private final MessageContentStrategyResolver contentStrategyResolver;
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

        // Strategy Pattern — เลือก strategy ตาม messageType แล้วปล่อยให้มันจัดการ
        // validation + transformation (TEXT: trim/ว่าง, IMAGE: base64 → S3, ...)
        Message.MessageType messageType = parseMessageType(payload.getMessageType());
        String content = contentStrategyResolver.resolve(messageType).process(payload.getContent());

        // ห้องกลุ่ม: broadcast อย่างเดียว — ไม่บันทึก DB, ไม่มี notification, ไม่มี unread
        // (ข้อความมีชีวิตอยู่เฉพาะตอนที่คนในห้องออนไลน์อยู่ด้วยกัน)
        if (room.getRoomType() == ChatRoom.RoomType.GROUP) {
            ChatMessageResponse response = new ChatMessageResponse(
                    UUID.randomUUID(),
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
        Message saved = messageRepository.save(message);
        messagingTemplate.convertAndSend("/topic/rooms/" + roomId + "/message-updates",
                (Object) java.util.Map.of("type", "DELETE", "roomId", roomId, "messageId", messageId));
        return toResponse(saved);
    }

    @Transactional
    public ChatMessageResponse edit(UUID userId, UUID roomId, UUID messageId, String content) {
        requireActiveMember(roomId,userId);
        Message message = messageRepository.findById(messageId)
            .filter(m -> m.getRoom().getId().equals(roomId) && m.getDeletedAt() == null)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Message not found"));
        if (!message.getSender().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only edit your own messages");
        if (message.getMessageType() != Message.MessageType.TEXT)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only text messages can be edited");
        if(content == null || content.isBlank() || content.length()>5000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Text must contain 1 to 5000 characters");
        message.setContent(contentStrategyResolver.resolve(Message.MessageType.TEXT).process(content));
        ChatMessageResponse result=toResponse(messageRepository.save(message));
        messagingTemplate.convertAndSend("/topic/rooms/"+roomId+"/message-updates", (Object)java.util.Map.of("type","EDIT","message",result));
        return result;
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
