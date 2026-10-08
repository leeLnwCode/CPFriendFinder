package com.cp.friend.config;

import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ตรวจสอบ authentication สำหรับ WebSocket/STOMP
 *
 * ลำดับความสำคัญ:
 * 1. Principal ที่ถูกตั้งแล้วตอน HTTP handshake (จาก HttpSessionHandshakeHandler)
 * 2. STOMP login header ที่ client ส่งมาตอน CONNECT (fallback)
 *
 * ทำให้ใช้ได้ทั้งกรณีที่ JSESSIONID cookie ถูกส่งมาตอน WS handshake
 * และกรณีที่ browser/js ไม่ส่ง cookie (พบบ่อยใน native WebSocket)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand cmd = accessor.getCommand();

        // log ทุก STOMP command เพื่อ debug
        log.info("[WS-Auth] STOMP command={} sessionId={} principal={}",
                cmd,
                accessor.getSessionId(),
                accessor.getUser() != null ? accessor.getUser().getName() : "null");

        if (cmd != StompCommand.CONNECT) {
            return message;
        }

        // --- CONNECT frame ---
        String login = accessor.getLogin();
        String sessionId = accessor.getSessionId();

        log.info("[WS-Auth] CONNECT received | sessionId={} | login-header='{}' | principal-before={}",
                sessionId,
                login != null ? login : "(none)",
                accessor.getUser() != null ? accessor.getUser().getName() : "null");

        // 1) Principal ถูกตั้งแล้วจาก handshake — ใช้เลย
        if (accessor.getUser() != null) {
            log.info("[WS-Auth] ✅ Principal from HTTP session handshake: {}", accessor.getUser().getName());
            return message;
        }

        // 2) Fallback: ดึง userId จาก STOMP login header
        if (login != null && !login.isBlank()) {
            try {
                UUID userId = UUID.fromString(login.trim());
                if (userRepository.existsById(userId)) {
                    accessor.setUser(() -> userId.toString());
                    log.info("[WS-Auth] ✅ Principal set from STOMP login header: {}", userId);
                } else {
                    log.warn("[WS-Auth] ❌ STOMP login userId not found in DB: {}", userId);
                }
            } catch (IllegalArgumentException e) {
                log.warn("[WS-Auth] ❌ STOMP login header is not a valid UUID: '{}'", login);
            }
        } else {
            log.warn("[WS-Auth] ❌ No principal from handshake AND no STOMP login header. Principal will be null!");
        }

        log.info("[WS-Auth] CONNECT result | sessionId={} | principal-after={}",
                sessionId,
                accessor.getUser() != null ? accessor.getUser().getName() : "null ← AUTH WILL FAIL");

        return message;
    }
}
