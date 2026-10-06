package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;

// Template Method Pattern — กำหนด "ลำดับการประมวลผล" ตายตัว (normalize → validate → transform)
// แต่ละ Strategy ขั้นย่อยเปลี่ยนเฉพาะขั้น validate/transform ที่เป็น hook
public abstract class AbstractMessageContentStrategy implements MessageContentStrategy {

    // Template Method — final เพื่อล็อกลำดับการทำงานให้ทุก Strategy เหมือนกัน
    @Override
    public final String process(String rawContent) {
        String content = normalize(rawContent);
        validate(content);
        return transform(content);
    }

    // implements ให้ลูกโดยอัตโนมัติ — ลูกบอกแค่ supportedType()
    @Override
    public final boolean supports(Message.MessageType type) {
        return supportedType() == type;
    }

    // hook ที่ใช้ร่วมกัน — คลาสลูก override ได้ถ้าต้องการ
    protected String normalize(String raw) {
        return raw == null ? "" : raw.trim();
    }

    // hook: ตรวจความถูกต้องเฉพาะชนิด — throw ResponseStatusException ถ้าไม่ผ่าน
    protected abstract void validate(String content);

    // hook: แปลงเนื้อหาเป็นสิ่งที่จะเก็บ (เช่น IMAGE → URL บน S3)
    protected abstract String transform(String content);

    // ให้ resolver หา strategy จาก type ได้
    public abstract Message.MessageType supportedType();
}
