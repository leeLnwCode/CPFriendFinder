package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cp.friend.model.ChatRoom;

// ข้อมูลย่อของห้อง — ใช้ในรายการห้อง (หน้า Home / ห้องของฉัน)
public record ChatRoomSummaryResponse(
        UUID id,
        String roomName,
        ChatRoom.RoomType roomType,
        boolean isPrivate,
        short maxMembers,
        long memberCount,
        long unreadCount,
        List<InterestDto> interests,
        Instant createdAt
) {

    public record InterestDto(UUID id, String name) {
    }
}
