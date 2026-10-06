package com.cp.friend.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.cp.friend.dto.request.ChatMessagePayload;
import com.cp.friend.dto.response.ChatMessageResponse;
import com.cp.friend.service.ChatMessageService;

import lombok.RequiredArgsConstructor;

// รับข้อความจาก WebSocket แล้ว broadcast ให้ทุกคนในห้อง
// client ส่งไปที่ /app/rooms/{roomId}/messages, ผู้รับ subscribe /topic/rooms/{roomId}
@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            ChatMessagePayload payload,
            Principal principal
    ) {
        if (principal == null) {
            throw new IllegalStateException("Authentication required");
        }

        ChatMessageResponse response =
                chatMessageService.send(UUID.fromString(principal.getName()), roomId, payload);

        messagingTemplate.convertAndSend("/topic/rooms/" + roomId, response);
    }
}
