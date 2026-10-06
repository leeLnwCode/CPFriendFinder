package com.cp.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Payload ที่ client ส่งเข้ามาทาง WebSocket (/app/rooms/{roomId}/messages)
@Getter
@Setter
public class ChatMessagePayload {

    @NotBlank(message = "content is required")
    @Size(max = 2000, message = "content must be at most 2000 characters")
    private String content;

    // "TEXT" | "IMAGE" | "FILE" — ไม่ส่งมาเป็น TEXT
    @Size(max = 10, message = "messageType must be at most 10 characters")
    private String messageType;
}
