package com.cp.friend.controller;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.service.ChatMessageService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chats/{roomId}/messages")
@RequiredArgsConstructor
public class ChatMessageController extends SessionController {

    private final ChatMessageService chatMessageService;

    // ส่งข้อความทาง REST (เหมาะกับรูป base64 ที่ใหญ่เกิน WS frame)
    // server จะ broadcast ให้คนอื่นในห้องผ่าน /topic/rooms/{roomId} เอง
    @PostMapping
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody ChatMessagePayload payload,
            HttpSession session
    ) {
        ChatMessageResponse response = chatMessageService.send(currentUserId(session), roomId, payload);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // จำนวนข้อความที่ยังไม่อ่านในห้องนี้
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(@PathVariable UUID roomId, HttpSession session) {
        return ResponseEntity.ok(chatMessageService.unreadCount(currentUserId(session), roomId));
    }

    // โหลดข้อความของห้อง (เก่า → ใหม่) — ?before=ISO instant สำหรับ scroll ย้อนหลัง &limit= (default 50)
    @GetMapping
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) String before,
            @RequestParam(defaultValue = "50") int limit,
            HttpSession session
    ) {
        Instant beforeInstant = parseBefore(before);
        return ResponseEntity.ok(
                chatMessageService.getMessages(currentUserId(session), roomId, beforeInstant, limit)
        );
    }

    // ลบข้อความของตัวเอง (soft delete)
    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            HttpSession session
    ) {
        chatMessageService.delete(currentUserId(session), roomId, messageId);
        return ResponseEntity.noContent().build();
    }

    private Instant parseBefore(String before) {
        if (before == null || before.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(before);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("before must be an ISO-8601 instant, e.g. 2026-01-01T00:00:00Z");
        }
    }
}
