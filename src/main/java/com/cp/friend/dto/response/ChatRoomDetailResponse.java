package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cp.friend.model.ChatRoom;
import com.cp.friend.model.RoomMember;

// รายละเอียดห้องเต็ม — ใช้ตอนเข้าห้อง (มีรายชื่อสมาชิกด้วย)
public record ChatRoomDetailResponse(
        UUID id,
        String roomName,
        String description,
        ChatRoom.RoomType roomType,
        boolean isPrivate,
        short maxMembers,
        long memberCount,
        List<ChatRoomSummaryResponse.InterestDto> interests,
        List<MemberDto> members,
        Instant createdAt
) {

    public record MemberDto(
            UUID userId,
            String firstname,
            String lastname,
            String imageUrl,
            RoomMember.Role role
    ) {
    }
}
