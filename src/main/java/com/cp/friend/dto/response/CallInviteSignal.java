package com.cp.friend.dto.response;

import java.util.UUID;

// สัญญาณสายเรียกเข้าที่ server ส่งตรงถึง /topic/call/{toUserId}
// INVITE    = มีสายเรียกเข้า (แสดง modal รับ/ปฏิเสธ พร้อมโปรไฟล์ผู้โทร)
// ACCEPT    = ผู้รับกดรับสาย — ผู้โทรนำทางไปหน้าแชทแล้วเริ่มคอล
// DECLINE   = ผู้รับกดปฏิเสธ
// CANCEL    = ผู้โทรยกเลิกก่อนมีคนรับ
public record CallInviteSignal(
        String type,
        UUID fromUserId,
        UUID toUserId,
        UUID roomId,
        String fromName,
        String fromImage,
        String mode
) {
}
