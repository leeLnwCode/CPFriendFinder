package com.cp.friend.dto.response;

import java.util.UUID;

public record FriendRemovalResponse(
        boolean success,
        String message,
        UUID friendId
) {
}
