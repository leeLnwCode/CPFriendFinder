package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.cp.friend.model.FriendRequest;

public record FriendRequestResponse(
        UUID id,
        UUID userId,
        String firstname,
        String lastname,
        String imageUrl,
        FriendRequest.Status status,
        Instant createdAt
) {
}
