package com.cp.friend.dto.response;

import java.util.UUID;

public record UserDiscoveryResponse(
        UUID id,
        String firstname,
        String lastname,
        String imageUrl,
        long commonInterestCount
) {
}
