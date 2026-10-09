package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// ConcreteStrategy — ข้อความธรรมดา: ต้องไม่ว่าง
@Component
public class TextContentStrategy extends AbstractMessageContentStrategy {

    @Override
    public Message.MessageType supportedType() {
        return Message.MessageType.TEXT;
    }

    @Override
    protected void validate(String content) {
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message content is required");
        }
    }

    @Override
    protected String transform(String content) {
        return content;
    }
}
