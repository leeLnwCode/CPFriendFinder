package com.cp.friend.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// สัญญาณ WebRTC ที่ client ส่งเข้ามาทาง /app/rooms/{roomId}/call
@Getter
@Setter
public class CallSignalRequest {

    // JOIN | LEAVE | OFFER | ANSWER | ICE
    private String type;

    // ต้องมีเมื่อ type = OFFER / ANSWER / ICE (จะส่งต่อให้ user นี้โดยเฉพาะ)
    private UUID targetUserId;

    // เนื้อหา SDP / ICE candidate (JSON string จาก RTCPeerConnection)
    private String payload;
}
