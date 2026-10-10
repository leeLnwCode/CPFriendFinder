# Design Patterns — CP Friend Finder

โครงงานนี้นำรูปแบบการออกแบบ (Design Patterns) กลุ่ม Behavioral มาใช้แก้ปัญหาจริง 3 รูปแบบ ทุกจุดอ้างอิงไฟล์โค้ดจริงในที่เก็บโครงงาน ดังนี้

---

## 1. Strategy Pattern — การจัดการเนื้อหาข้อความหลายประเภท

**ปัญหา:** ระบบแชทรองรับข้อความ 3 ประเภท (TEXT / IMAGE / FILE) ซึ่งแต่ละประเภทมีกฎการตรวจสอบและการแปลงเนื้อหาต่างกัน หากเขียนเป็น if-else ใน `ChatMessageService` โค้ดจะบวมและแก้ไขยากทุกครั้งที่เพิ่มประเภทใหม่

**โครงสร้าง:**
- `service/strategy/MessageContentStrategy.java` — อินเทอร์เฟซกลาง 2 เมธอด: `supports(MessageType)` และ `process(String rawContent)`
- `service/strategy/AbstractMessageContentStrategy.java` — คลาสแม่ที่เก็บ `supportedType()` ให้ลูกทุกตัวรู้จักประเภทของตัวเอง
- `service/strategy/TextContentStrategy.java`, `ImageContentStrategy.java`, `FileContentStrategy.java` — กฎการประมวลผลของแต่ละประเภท
- `service/strategy/MessageContentStrategyResolver.java` — ตัวเลือกกลยุทธ์ รับ Strategy ทั้งหมดที่ Spring inject เข้ามาเป็น `List` (constructor injection) เก็บลง `EnumMap` แล้วเลือกให้ถูกชนิดด้วย `resolve(MessageType)`

**เหตุผลและประโยชน์:** เพิ่มชนิดข้อความใหม่ (เช่น VIDEO) = เพิ่มคลาส Strategy ใหม่หนึ่งไฟล์ Resolver จะจับคู่ให้เองโดยอัตโนมัติ ไม่ต้องแก้ if-else ใน `ChatMessageService` เลย — สอดคล้องกับหลักการ Open/Closed Principle (คอมเมนต์เจตนาการออกแบบระบุไว้ในไฟล์ต้นฉบับ)

**แผนภาพประกอบ:** `doc/diagrams/03-class-diagram.png`

---

## 2. Observer Pattern — ระบบแจ้งเตือนแบบกระจายเหตุการณ์

**ปัญหา:** เมื่อเกิดเหตุการณ์ เช่น มีการส่ง/ยอมรับ/ปฏิเสธคำขอเป็นเพื่อน ฝั่ง `FriendRequestService` ต้องให้ระบบแจ้งเตือนทำงานตาม แต่การเรียก `NotificationService` โดยตรงจะทำให้สองส่วนผูกกันแน่น (tight coupling)

**โครงสร้าง:**
- `event/FriendRequestSentEvent.java`, `event/FriendRequestAcceptedEvent.java`, `event/FriendRequestDeclinedEvent.java` — เหตุการณ์ (Subject ประกาศ "มีอะไรเกิดขึ้น" โดยไม่สนใครสนใจ)
- `service/FriendRequestService.java` — ผู้เผยแพร่เหตุการณ์ (Publisher) ผ่าน `ApplicationEventPublisher.publishEvent(...)` — ตามโค้ดจริงที่บรรทัด `publishEvent(new FriendRequestSentEvent(request))` และ `publishEvent(new FriendRequestAcceptedEvent(...))`
- `service/NotificationService.java` — ผู้สังเกตการณ์ (Observer) มีเมธอด `@EventListener` 3 ตัว: `onFriendRequestSent`, `onFriendRequestAccepted`, `onFriendRequestDeclined` รับเหตุการณ์แล้วสร้างการแจ้งเตือนให้ผู้รับโดยอัตโนมัติ
- `service/RoomRealtimeService.java` — Observer อีกตัวที่ฟังเหตุการณ์เดียวกันเพื่อผลักข้อมูลแบบเรียลไทม์

**เหตุผลและประโยชน์:** `FriendRequestService` publish event แล้วไม่รู้จัก `NotificationService` เลย (ตามคอมเมนต์ในโค้ด) — ลดการเชื่อมโยงระหว่างส่วน เพิ่ม Observer ใหม่ (เช่น ผลักการแจ้งเตือนเข้า WebSocket) ได้โดยไม่แก้โค้ดเดิม

**แผนภาพประกอบ:** `doc/diagrams/04-sequence-friend-request.png`

---

## 3. State Pattern — การจัดการสถานะคำขอเป็นเพื่อน

**ปัญหา:** คำขอเป็นเพื่อนมีวงจรสถานะ PENDING → ACCEPTED หรือ DECLINED พฤติกรรมที่อนุญาตเปลี่ยนตามสถานะ เช่น คำขอที่ตอบกลับไปแล้วห้ามตอบซ้ำ

**โครงสร้าง:**
- `model/FriendRequest.java` — ฟิลด์ `status` เป็น enum `FriendRequest.Status { PENDING, ACCEPTED, DECLINED }` และฐานข้อมูลกำหนด CHECK constraint กันค่านอกชุดนี้
- `service/FriendRequestService.java` — จุดเปลี่ยนสถานะทั้งหมดรวมศูนย์ที่เดียว: `create()` ตั้ง PENDING / `accept()` และ `decline()` มีเงื่อนไขป้องกัน (guard) ว่าสถานะปัจจุบันต้องเป็น PENDING เท่านั้น (`request.getStatus() != PENDING` → ปฏิเสธการทำงาน) ก่อนเปลี่ยนสถานะและ publish เหตุการณ์ให้ Observer

**เหตุผลและประโยชน์:** พฤติกรรมของระบบเปลี่ยนตามสถานะปัจจุบันอย่างชัดเจน การเปลี่ยนสถานะที่ไม่ถูกต้องถูกบล็อกตั้งแต่ชั้น Service พร้อม constraint กันที่ฐานข้อมูลเป็นชั้นสำรอง ทำให้ตรวจสอบและทดสอบได้ง่าย

**แผนภาพประกอบ:** `doc/diagrams/03-class-diagram.png`, `doc/diagrams/08-state-diagram.png` (วงจรสถานะของระบบสุ่มคุยซึ่งใช้แนวคิดเดียวกัน)

---

## สรุปความสัมพันธ์กับหลักการ SOLID

| Pattern | ไฟล์หลัก | หลักการที่เสริม |
|---|---|---|
| Strategy | `service/strategy/*` | Open/Closed Principle |
| Observer | `event/*`, `service/NotificationService.java`, `service/FriendRequestService.java` | Single Responsibility, Loose Coupling |
| State | `model/FriendRequest.java`, `service/FriendRequestService.java` | ความถูกต้องของพฤติกรรมตามสถานะ |

ดูการวิเคราะห์หลักการ SOLID ทั้ง 5 ข้อได้ที่ `doc/solid-analysis.md`
