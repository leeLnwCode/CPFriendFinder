package com.cp.friend.service.strategy;

import com.cp.friend.model.Message;

// Strategy Pattern — กฎการประมวลผลเนื้อหาข้อความแยกตาม messageType
// เพิ่มชนิดข้อความใหม่ (เช่น VIDEO) = เพิ่มคลาส Strategy ใหม่ ไม่ต้องแก้ if-else ใน ChatMessageService (Open/Closed)
public interface MessageContentStrategy {

    // ชนิดข้อความที่ใช้ลงทะเบียน strategy โดยไม่ผูกกับคลาสแม่
    Message.MessageType supportedType();

    // Strategy นี้รองรับ messageType ใด
    boolean supports(Message.MessageType type);

    // แปลงเนื้อหาดิบจาก client เป็น content ที่จะบันทึก/broadcast
    String process(String rawContent);
}
