# SOLID Analysis — CP Friend Finder

เอกสารวิเคราะห์การใช้หลักการ SOLID กับโค้ดจริงของโครงงาน โดยระบุ Principle → ไฟล์ → คลาส/จุดโค้ด → เหตุผล
ระบบพัฒนาบน Spring Boot ตามสถาปัตยกรรมแบบแบ่งชั้น: `controller → service → repository → model (entity)`

---

## S — Single Responsibility Principle (คลาสหนึ่งมีเหตุผลเดียวในการเปลี่ยนแปลง)

| จุดใช้งาน | ไฟล์ / คลาส | เหตุผล |
|---|---|---|
| แยกชั้นตามหน้าที่ | `controller/` (17 คลาส), `service/` (15 คลาส), `repository/` (12 อินเทอร์เฟซ), `model/` (14 Entity) | Controller รับคำขอ/ส่ง response เท่านั้น, Service รวมศูนย์ตรรกะทางธุรกิจ, Repository สื่อสารกับฐานข้อมูลเท่านั้น — Controller ห้ามเรียก Repository โดยตรง |
| บริการแยกตามโดเมน | `service/FriendRequestService.java`, `service/NotificationService.java`, `service/MatchingService.java`, `service/ChatRoomService.java`, ฯลฯ | แต่ละ Service ดูแลเรื่องของตัวเองเพียงเรื่อง เช่น การเปลี่ยนกติกาคำขอเป็นเพื่อนกระทบเฉพาะ `FriendRequestService` ไม่กระทบระบบแชท |
| แยกหน้าที่ระดับคลาสย่อย | `service/strategy/MessageContentStrategyResolver.java` | ทำหน้าที่เดียวคือเลือก Strategy ให้ถูกประเภท ไม่รู้เรื่องกฎการประมวลผลเนื้อหาจริง |
| แยกการกำหนดค่า | `config/WebSocketConfig.java`, `config/S3Config.java`, `config/SecurityConfig.java` | การตั้งค่าแต่ละโดเมนอยู่ config ของตัวเอง |

## O — Open/Closed Principle (เปิดรับการขยาย ปิดการแก้ไขของเดิม)

| จุดใช้งาน | ไฟล์ / คลาส | เหตุผล |
|---|---|---|
| เพิ่มประเภทข้อความใหม่ | `service/strategy/` — `MessageContentStrategy` + `AbstractMessageContentStrategy` + `Text/Image/FileContentStrategy` + `MessageContentStrategyResolver` | เพิ่มชนิดข้อความใหม่ (เช่น VIDEO) = เพิ่มคลาส Strategy ใหม่ 1 ไฟล์ Resolver จับคู่ให้เองผ่าน constructor injection ของ `List<MessageContentStrategy>` ไม่ต้องแก้ if-else ใน `ChatMessageService` |

## L — Liskov Substitution Principle (คลาสลูกแทนคลาสแม่ได้โดยระบบไม่พัง)

| จุดใช้งาน | ไฟล์ / คลาส | เหตุผล |
|---|---|---|
| ทุก Strategy ใช้แทนกันได้ | `TextContentStrategy`, `ImageContentStrategy`, `FileContentStrategy` สืบทอด `AbstractMessageContentStrategy` ซึ่ง implements `MessageContentStrategy` | `ChatMessageService` ใช้งานผ่านประเภทอินเทอร์เฟซเท่านั้น (`resolver.resolve(type)` คืนอะไรมาก็เรียก `process()` ได้) สลับคลาสกันได้โดยพฤติกรรมถูกต้องตามสัญญาของอินเทอร์เฟซ |

## I — Interface Segregation Principle (ไม่บังคับให้คลาสทำงานที่ตนไม่ใช้)

| จุดใช้งาน | ไฟล์ / คลาส | เหตุผล |
|---|---|---|
| อินเทอร์เฟซเล็กและเฉพาะเรื่อง | `service/strategy/MessageContentStrategy.java` (มีแค่ 2 เมธอด: `supports`, `process`) | ผู้ใช้ Strategy ไม่ต้องรู้/ทำงานกับเมธอดที่ไม่เกี่ยว เช่น ไม่ถูกบังคับให้เกี่ยวกับการบันทึกหรือ broadcast |
| Repository แยกตาม Entity | `repository/UserRepository.java`, `repository/FriendRequestRepository.java`, ฯลฯ (12 อินเทอร์เฟซ) | แต่ละ Service เห็นเฉพาะ Repository ของข้อมูลที่ตนใช้ ไม่มี Repository ยักษ์ตัวเดียวที่ทุกส่วนต้องพึ่ง |

## D — Dependency Inversion Principle (พึ่งนามธรรม ไม่พึ่งของจริงโดยตรง)

| จุดใช้งาน | ไฟล์ / คลาส | เหตุผล |
|---|---|---|
| Constructor Injection ทั้งระบบ | ทุก Service และ Controller เช่น `service/FriendRequestService.java` (inject Repository + `ApplicationEventPublisher` ผ่าน constructor, field เป็น `final`) | ชั้นบนพึ่งนิยาม (interface/abstract) ไม่สร้าง object จริงเอง — Spring ส่งของจริงเข้ามาตอน runtime ทำให้ mock ทดแทนตอนทดสอบได้ (ดู `src/test/...` ที่ใช้ Mockito) |
| Service พึ่งอินเทอร์เฟซของ Strategy | `ChatMessageService` → `MessageContentStrategyResolver` → `MessageContentStrategy` (interface) | Service ไม่รู้จักคลาส Text/Image/File โดยตรง |
| Observer ผ่าน `ApplicationEventPublisher` | `FriendRequestService` publish event, `NotificationService` รับด้วย `@EventListener` | ผู้ publish พึ่งกลไก event ของ Spring (นามธรรม) ไม่พึ่งคลาสผู้ฟังโดยตรง |

---

## หลักฐานประกอบ

- แผนภาพคลาสที่แสดงโครงสร้าง Pattern: `doc/diagrams/03-class-diagram.png`
- รายละเอียด Pattern: `doc/design-patterns.md`
- ตัวอย่างการทดสอบที่อาศัย DIP (mock ผ่าน constructor): `src/test/java/com/cp/friend/service/`, ผลรวม 29 ไฟล์ทดสอบ
- รายการเหตุการณ์ของระบบแจ้งเตือน: `src/main/java/com/cp/friend/event/` (Sent / Accepted / Declined)
