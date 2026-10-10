package com.cp.friend.dto.response;

import java.util.UUID;

// DTO แทนการคืน Interest entity ตรงๆ — แยก API contract ออกจาก domain (Layered/DTO Pattern)
public record InterestResponse(
        UUID id,
        String name,
        boolean isActive
) {
}
