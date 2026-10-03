package com.cp.friend.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

// Base class สำหรับ controller ที่ใช้ session-based auth
// ทุก controller เรียก currentUserId(session) เพื่อเอา userId ของผู้ใช้ที่ login อยู่
public abstract class SessionController {

    protected UUID currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof UUID id) {
            return id;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
