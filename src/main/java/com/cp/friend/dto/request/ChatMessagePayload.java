package com.cp.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// Payload สำหรับส่งข้อความ — ทาง WebSocket (/app/rooms/{roomId}/messages)
// หรือ REST (POST /api/chats/{roomId}/messages)
// รูป (messageType = IMAGE) ส่งเป็น base64 ใน content — แนะนำให้ใช้ REST เพราะขนาดใหญ่
@Getter
@Setter
public class ChatMessagePayload {

    @NotBlank(message = "content is required")
    @Size(max = 4_000_000, message = "content is too large (max ~3MB image)")
    private String content;

    // "TEXT" | "IMAGE" | "FILE" — ไม่ส่งมาเป็น TEXT
    @Size(max = 10, message = "messageType must be at most 10 characters")
    private String messageType;
}
