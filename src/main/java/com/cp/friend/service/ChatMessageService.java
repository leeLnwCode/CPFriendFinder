package com.cp.friend.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 100;

    private final MessageRepository messageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoomMemberRepository roomMemberRepository;

    // =========================================================
    // โหลดข้อความ (เฉพาะสมาชิกห้อง)
    // =========================================================

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(UUID userId, UUID roomId, Instant before, int limit) {
        requireActiveMember(roomId, userId);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), MAX_PAGE_SIZE));

        List<Message> messages = before == null
                ? messageRepository.findLatestByRoomId(roomId, pageable)
                : messageRepository.findByRoomIdBefore(roomId, before, pageable);

        // จัดเรียงเก่า → ใหม่ เพื่อให้ client แสดงต่อท้ายได้เลย
        return messages.reversed().stream()
                .map(ChatMessageService::toResponse)
                .toList();
    }

    // =========================================================
    // ส่งข้อความ (ใช้ทั้งจาก WebSocket และ REST) — คืน response เพื่อ broadcast
    // =========================================================

    @Transactional
    public ChatMessageResponse send(UUID userId, UUID roomId, ChatMessagePayload payload) {
        requireActiveMember(roomId, userId);

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));

        Message message = new Message();
        message.setRoom(room);
        message.setSender(roomMemberRepository.findActiveMember(roomId, userId)
                .map(RoomMember::getUser)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this room")));
        message.setContent(payload.getContent().trim());
        message.setMessageType(parseMessageType(payload.getMessageType()));

        return toResponse(messageRepository.save(message));
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

    private void requireActiveMember(UUID roomId, UUID userId) {
        if (!roomMemberRepository.isActiveMember(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not a member of this room");
        }
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
