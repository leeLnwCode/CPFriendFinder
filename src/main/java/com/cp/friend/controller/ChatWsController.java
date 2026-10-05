package com.cp.friend.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.service.ChatMessageService;

import lombok.RequiredArgsConstructor;

// WebSocket endpoints:
// - ข้อความ: ส่งที่ /app/rooms/{roomId}/messages, ผู้รับ subscribe /topic/rooms/{roomId}
//   (ส่งรูปควรใช้ REST POST /api/chats/{roomId}/messages แทน เพราะ base64 ใหญ่เกิน frame ของ WS)
// - วิดีโอคอล (WebRTC signaling): ส่งที่ /app/rooms/{roomId}/call
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            ChatMessagePayload payload,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required");
        }
        chatMessageService.send(UUID.fromString(principal.getName()), roomId, payload);
    }

    // ส่งต่อสัญญาณ WebRTC (JOIN/LEAVE/OFFER/ANSWER/ICE) — ตรวจสิทธิ์สมาชิกห้องก่อน relay
    @MessageMapping("/rooms/{roomId}/call")
    public void handleCallSignal(
            @DestinationVariable UUID roomId,
            CallSignalRequest signal,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required");
        }
        chatMessageService.relayCallSignal(UUID.fromString(principal.getName()), roomId, signal);
    }
}
