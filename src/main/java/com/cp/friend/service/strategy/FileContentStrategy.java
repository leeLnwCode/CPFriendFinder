package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// ConcreteStrategy — ไฟล์แนบ: ผ่านตามที่ส่งมา (ยังไม่ทำ upload จริง)
@Component
public class FileContentStrategy extends AbstractMessageContentStrategy {

    @Override
    public Message.MessageType supportedType() {
        return Message.MessageType.FILE;
    }

    @Override
    protected void validate(String content) {
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File content is required");
        }
    }

    @Override
    protected String transform(String content) {
        return content;
    }
}
