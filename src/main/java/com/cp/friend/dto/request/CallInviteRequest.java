package com.cp.friend.dto.request;

import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

// สัญญาณ "โทรเรียก/รับสาย/ปฏิเสธ/ยกเลิก" ที่ client ส่งเข้ามาทาง /app/call
@Getter
@Setter
public class CallInviteRequest {

    // INVITE | ACCEPT | DECLINE | CANCEL
    private String type;

    // VOICE | VIDEO (missing mode defaults to VOICE for older clients)
    private String mode;

    // ผู้รับสัญญาณ (เพื่อนที่โทรหา/ตอบกลับ)
    private UUID toUserId;

    // ห้อง direct ที่ใช้คุย — ต้องมีเมื่อ INVITE (ใช้ตรวจสิทธิ์ + ให้ผู้รับใช้เข้าห้อง)
    private UUID roomId;
}
