package com.cp.friend.mapper;

import org.springframework.stereotype.Component;

import com.cp.friend.dto.response.UpdateProfileResponse;
import com.cp.friend.model.User;

// Mapper Pattern — Entity (User) → Profile Response DTO ที่เดียว
// controller ไม่ต้องประกอบ DTO เอง
@Component
public class UserMapper {

    public UpdateProfileResponse toProfileResponse(User user) {
        return new UpdateProfileResponse(user);
    }
}
