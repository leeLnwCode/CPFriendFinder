package com.cp.friend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// WebSocket + STOMP สำหรับ real-time chat
//
// ฝั่ง client:
// - เชื่อมต่อ: ws(s)://<host>/ws (native WebSocket — ไม่ใช้ SockJS กัน warning
//   "Permissions policy violation: unload" ที่มาจาก sockjs-client)
// - ส่งข้อความ: /app/rooms/{roomId}/messages พร้อม JSON {content, messageType?}
// - รับข้อความ: subscribe /topic/rooms/{roomId}
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new HttpSessionHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }
}
