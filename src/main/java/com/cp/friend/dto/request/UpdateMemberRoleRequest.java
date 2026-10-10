package com.cp.friend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRoleRequest {

    // "MODERATOR" หรือ "MEMBER" — ย้าย OWNER ยังไม่รองรับ
    @NotBlank(message = "role is required")
    private String role;
}
