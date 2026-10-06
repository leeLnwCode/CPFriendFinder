package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

// Resolver — รับ Strategy ทั้งหมดที่ Spring inject เข้ามา (constructor injection ของ List)
// แล้วเลือกให้ถูกชนิด: เพิ่ม messageType ใหม่ = เพิ่ม Strategy bean ตัวนี้อัปเดตเองอัตโนมัติ (Open/Closed)
@Component
public class MessageContentStrategyResolver {

    private final Map<Message.MessageType, MessageContentStrategy> strategies = new EnumMap<>(Message.MessageType.class);

    public MessageContentStrategyResolver(List<MessageContentStrategy> candidates) {
        for (MessageContentStrategy strategy : candidates) {
            if (strategy instanceof AbstractMessageContentStrategy abstractStrategy) {
                strategies.put(abstractStrategy.supportedType(), strategy);
            }
        }
    }

    public MessageContentStrategy resolve(Message.MessageType type) {
        MessageContentStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported messageType: " + type);
        }
        return strategy;
    }
}
