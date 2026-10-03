package com.cp.friend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinChatRoomRequest {

    // ต้องส่งเมื่อห้องเป็น private
    @Size(max = 100, message = "password must be at most 100 characters")
    private String password;
}
