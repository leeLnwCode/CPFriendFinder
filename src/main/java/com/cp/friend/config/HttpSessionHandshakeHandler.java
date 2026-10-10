package com.cp.friend.config;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpSession;

// ดึง userId จาก HttpSession (cookie JSESSIONID ตอน handshake)
// แล้วตั้งเป็น Principal ของ WebSocket connection — handler ใช้รู้ว่าใครส่งข้อความ
public class HttpSessionHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            if (session != null && session.getAttribute("userId") instanceof UUID userId) {
                return () -> userId.toString();
            }
        }
        return null;
    }
}
