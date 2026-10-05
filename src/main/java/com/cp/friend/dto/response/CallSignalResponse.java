package com.cp.friend.dto.response;

import java.util.UUID;

// สัญญาณที่ server ส่งต่อให้ client
// - JOIN/LEAVE  broadcast ไปที่ /topic/rooms/{roomId}/call ให้ทุกคนในห้องเห็น
// - OFFER/ANSWER/ICE  ส่งตรงถึงเป้าหมายที่ /user/queue/call
public record CallSignalResponse(
        String type,
        UUID fromUserId,
        String payload
) {
}
