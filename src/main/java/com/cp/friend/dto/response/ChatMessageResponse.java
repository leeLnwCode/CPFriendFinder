package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.cp.friend.model.Message;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        UUID senderId,
        String senderFirstname,
        String senderLastname,
        String senderImageUrl,
        String content,
        Message.MessageType messageType,
        Instant createdAt
) {
}
