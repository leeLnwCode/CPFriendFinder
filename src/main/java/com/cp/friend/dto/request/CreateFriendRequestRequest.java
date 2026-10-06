package com.cp.friend.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateFriendRequestRequest(
        @NotNull(message = "receiverId is required")
        UUID receiverId
) {
}
