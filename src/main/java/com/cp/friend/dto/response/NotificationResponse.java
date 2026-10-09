package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.cp.friend.model.Notification;

public record NotificationResponse(
        UUID id,
        Notification.Type type,
        String title,
        String message,
        boolean isRead,
        Instant createdAt,
        UUID friendRequestId,
        UUID roomId,
        ActorDto actor
) {

    // คนที่ก่อให้เกิดการแจ้งเตือน เช่น คนที่ส่ง friend request
    public record ActorDto(
            UUID id,
            String firstname,
            String lastname,
            String imageUrl
    ) {
    }
}
