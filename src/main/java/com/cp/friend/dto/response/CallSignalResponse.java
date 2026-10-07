package com.cp.friend.dto.response;

import java.util.UUID;

// สัญญาณที่ server ส่งต่อให้ client — broadcast ทั้งหมดที่ /topic/rooms/{roomId}/call
// OFFER/ANSWER/ICE มี toUserId ระบุผู้รับ — client ที่ไม่ใช่ผู้รับต้องข้าม message นั้น
// (ใช้ broadcast แทน /user/queue เพราะ user-destination ยังส่งไม่ได้ใน Spring Boot 4.1.1)
public record CallSignalResponse(
        String type,
        UUID fromUserId,
        UUID toUserId,
        String payload
) {
}
