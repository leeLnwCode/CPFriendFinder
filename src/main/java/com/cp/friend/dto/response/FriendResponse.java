package com.cp.friend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
        UUID friendId,
        String firstname,
        String lastname,
        String imageUrl,
        Instant friendsSince
) {
}
