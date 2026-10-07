package com.cp.friend.dto.request;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// แก้ไขห้อง — ส่งมาแค่ field ที่ต้องการแก้ (null = ไม่แก้)
// เฉพาะ OWNER / MODERATOR เท่านั้น
@Getter
@Setter
public class UpdateChatRoomRequest {

    @Size(max = 100, message = "roomName must be at most 100 characters")
    private String roomName;

    @Size(max = 500, message = "description must be at most 500 characters")
    private String description;

    @Min(value = 2, message = "maxMembers must be at least 2")
    @Max(value = 100, message = "maxMembers must be at most 100")
    private Short maxMembers;

    private Boolean isPrivate;

    // ตั้ง/เปลี่ยนรหัสห้อง — ได้เฉพาะเมื่อห้องเป็น private
    @Size(min = 4, max = 100, message = "password must be between 4 and 100 characters")
    private String password;

    // ส่งมา = แทนที่ interest ทั้งหมดของห้อง (ส่ง [] = ล้าง)
    private List<UUID> interestIds;
}
