package com.cp.friend.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateUserInterestsRequest(
        @NotNull(message = "interestIds is required")
        List<@NotNull(message = "interestIds must not contain null") UUID> interestIds
) {
}
