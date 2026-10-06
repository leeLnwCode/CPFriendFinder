package com.cp.friend.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChatRoomRequest {

    @NotBlank(message = "roomName is required")
    @Size(max = 100, message = "roomName must be at most 100 characters")
    private String roomName;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    // ชั้นปีเป้าหมายของห้อง — null = ทุกชั้นปี
    @Positive(message = "targetYear must be a positive number")
    private Short targetYear;

    // interest ของห้อง (optional) — ใช้ filter หน้า Home
    private List<UUID> interestIds;

    @Min(value = 2, message = "maxMembers must be at least 2")
    @Max(value = 100, message = "maxMembers must be at most 100")
    private Short maxMembers;

    private boolean isPrivate = false;

    // ต้องส่งเมื่อ isPrivate = true
    @Size(min = 4, max = 100, message = "password must be between 4 and 100 characters")
    private String password;
}
