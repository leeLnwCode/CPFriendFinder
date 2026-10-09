package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;
import com.cp.friend.port.StoragePort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// ConcreteStrategy — รูปภาพ: ตรวจขนาด แล้วอัปโหลดขึ้น S3 ผ่าน StoragePort (Adapter)
// content ที่เก็บคือ URL ของไฟล์ ไม่ใช่ base64
@Component
public class ImageContentStrategy extends AbstractMessageContentStrategy {

    private static final int MAX_IMAGE_BASE64_LENGTH = 4_000_000; // ~3MB binary

    private final StoragePort storagePort;

    public ImageContentStrategy(StoragePort storagePort) {
        this.storagePort = storagePort;
    }

    @Override
    public Message.MessageType supportedType() {
        return Message.MessageType.IMAGE;
    }

    @Override
    protected void validate(String content) {
        if (content.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image content is required");
        }
        if (content.length() > MAX_IMAGE_BASE64_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is too large (max ~3MB)");
        }
    }

    @Override
    protected String transform(String content) {
        return storagePort.publicUrl(storagePort.uploadBase64(content));
    }
}
