package com.cp.friend.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import com.cp.friend.dto.request.CallInviteRequest;
import com.cp.friend.dto.request.CallSignalRequest;
import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.service.ChatMessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// WebSocket endpoints:
// - ข้อความ: ส่งที่ /app/rooms/{roomId}/messages, ผู้รับ subscribe /topic/rooms/{roomId}
// - วิดีโอคอล (WebRTC signaling): ส่งที่ /app/rooms/{roomId}/call
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatMessageService chatMessageService;
    private final com.cp.friend.service.RoomRealtimeService realtime;

    @MessageMapping("/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            ChatMessagePayload payload,
            Principal principal
    ) {
        log.info("[WS] /rooms/{}/messages | principal={} | type={}",
                roomId,
                principal != null ? principal.getName() : "null ← NO AUTH",
                payload != null ? payload.getMessageType() : "null");

        if (principal == null) {
            log.error("[WS] ❌ Authentication required on /rooms/{}/messages", roomId);
            throw new IllegalStateException("Authentication required");
        }
        chatMessageService.send(UUID.fromString(principal.getName()), roomId, payload);
    }

    // ส่งต่อสัญญาณ WebRTC (JOIN/LEAVE/OFFER/ANSWER/ICE)
    @MessageMapping("/rooms/{roomId}/call")
    public void handleCallSignal(
            @DestinationVariable UUID roomId,
            CallSignalRequest signal,
            @org.springframework.messaging.handler.annotation.Header("simpSessionId") String sessionId,
            Principal principal
    ) {
        String signalType = signal != null ? signal.getType() : "null";
        String targetId   = signal != null && signal.getTargetUserId() != null
                ? signal.getTargetUserId().toString() : "broadcast";

        // ตัด SDP ให้สั้นลงเพื่อไม่ให้ log ยาวเกิน
        String payloadPreview = "";
        if (signal != null && signal.getPayload() != null) {
            String p = signal.getPayload();
            payloadPreview = p.length() > 120 ? p.substring(0, 120) + "…" : p;
        }

        log.info("[WS] /rooms/{}/call | principal={} | type={} | target={} | payload={}",
                roomId,
                principal != null ? principal.getName() : "null ← NO AUTH",
                signalType,
                targetId,
                payloadPreview);

        if (principal == null) {
            log.error("[WS] ❌ Authentication required on /rooms/{}/call | signalType={}", roomId, signalType);
            throw new IllegalStateException("Authentication required");
        }
        chatMessageService.relayCallSignal(UUID.fromString(principal.getName()), roomId, signal);
        realtime.signal(roomId, UUID.fromString(principal.getName()), sessionId, signal.getType(), signal.getPayload());
    }

    // สายเรียกเข้า: INVITE/ACCEPT/DECLINE/CANCEL
    @MessageMapping("/call")
    public void handleCallInvite(CallInviteRequest request, Principal principal) {
        log.info("[WS] /call | principal={} | action={}",
                principal != null ? principal.getName() : "null ← NO AUTH",
                request != null ? request.getType() : "null");

        if (principal == null) {
            log.error("[WS] ❌ Authentication required on /call");
            throw new IllegalStateException("Authentication required");
        }
        chatMessageService.relayCallInvite(UUID.fromString(principal.getName()), request);
    }
}
