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
        try {
            if (!content.matches("(?s)^data:image/(png|jpeg);base64,.+")) throw new IllegalArgumentException();
            byte[] bytes=java.util.Base64.getDecoder().decode(content.substring(content.indexOf(',')+1));
            if(bytes.length>3_000_000) throw new IllegalArgumentException();
            try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers=javax.imageio.ImageIO.getImageReaders(input);
                if(!readers.hasNext()) throw new IllegalArgumentException();
                var reader=readers.next();
                try {reader.setInput(input);if(reader.getWidth(0)>4096 || reader.getHeight(0)>4096 || reader.read(0)==null)throw new IllegalArgumentException();}
                finally {reader.dispose();}
            }
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid JPEG/PNG image, max 3 MB and 4096 pixels per side");
        }
    }

    @Override
    protected String transform(String content) {
        return storagePort.publicUrl(storagePort.uploadBase64(content));
    }
}
