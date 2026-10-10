package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cp.friend.model.FriendRequest;

// ข้อมูล "อีกฝั่ง" ของ friend request — ใช้แสดง popup รับสาย/คำขอเพื่อน
// (userId/firstname/.../year/.../interests คือข้อมูลของอีกฝั่ง ไม่ใช่ผู้ถือ request)
public record FriendRequestResponse(
        UUID id,
        UUID userId,
        String firstname,
        String lastname,
        String imageUrl,
        Short year,
        String department,
        String bio,
        List<InterestResponse> interests,
        FriendRequest.Status status,
        Instant createdAt
) {
}
