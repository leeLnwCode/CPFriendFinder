# การวิเคราะห์ SOLID Principles

[SOLID](solid-analysis.md) · [Design Patterns](design-patterns.md) · [เอกสารการทดสอบ](../test/README.md)

ระบบ CPFriendFinder ใช้หลัก SOLID ในการแบ่งหน้าที่ของคลาส แยกส่วนติดต่อผู้ใช้ กฎการทำงาน การเข้าถึงข้อมูล และบริการภายนอก เพื่อให้ดูแลโค้ดและทดสอบแต่ละส่วนได้สะดวก

## การประยุกต์ใช้ SOLID พร้อมตำแหน่งโค้ด

| หลักการ | ไฟล์ / บรรทัด | เหตุผลและการใช้งาน |
|---|---|---|
| S — Single Responsibility | [CallSignalingService.java](../src/main/java/com/cp/friend/service/CallSignalingService.java); [ChatMessageController.java:32](../src/main/java/com/cp/friend/controller/ChatMessageController.java#L32); [ImageContentStrategy.java:28](../src/main/java/com/cp/friend/service/strategy/ImageContentStrategy.java#L28); [StorageTool.java:18](../src/main/java/com/cp/friend/tools/StorageTool.java#L18); [MessageRepository.java:16](../src/main/java/com/cp/friend/repository/MessageRepository.java#L16) | Controller ประสาน HTTP; strategy ตรวจและแปลงภาพ; storage adapter ติดต่อบริการไฟล์; repository เข้าถึงข้อมูล แยกหน้าที่ส่วนหลักออกจากกัน |
| O — Open/Closed | [MessageContentStrategyResolver.java:19](../src/main/java/com/cp/friend/service/strategy/MessageContentStrategyResolver.java#L19); [AbstractMessageContentStrategy.java:11](../src/main/java/com/cp/friend/service/strategy/AbstractMessageContentStrategy.java#L11); [MatchingServiceImpl.java:34](../src/main/java/com/cp/friend/service/impl/MatchingServiceImpl.java#L34) | การประมวลผลข้อความลงทะเบียนผ่าน MessageContentStrategy.supportedType() โดย resolver พึ่ง interface โดยตรง คลาสที่ต้องการใช้ขั้นตอนร่วมกันสามารถสืบทอด AbstractMessageContentStrategy ได้ ส่วน MatchingServiceImpl เรียกอัลกอริทึมผ่าน MatchingStrategy จึงแยกการเลือกวิธีทำงานออกจากขั้นตอนหลัก |
| L — Liskov Substitution | [MessageContentStrategy.java:7](../src/main/java/com/cp/friend/service/strategy/MessageContentStrategy.java#L7); [TextContentStrategy.java:10](../src/main/java/com/cp/friend/service/strategy/TextContentStrategy.java#L10); [ImageContentStrategy.java:12](../src/main/java/com/cp/friend/service/strategy/ImageContentStrategy.java#L12); [AbstractMessageContentStrategy.java:11](../src/main/java/com/cp/friend/service/strategy/AbstractMessageContentStrategy.java#L11) | TextContentStrategy, ImageContentStrategy และ FileContentStrategy ใช้งานผ่าน MessageContentStrategy โดยมีเมธอด process ที่คืน String เหมือนกัน คลาสแม่กำหนดลำดับ normalize → validate → transform ส่วนคลาสลูกกำหนดการตรวจสอบและแปลงข้อมูลตามชนิดข้อความ |
| I — Interface Segregation | [StoragePort.java:6](../src/main/java/com/cp/friend/port/StoragePort.java#L6); [MessageNotifications.java:3](../src/main/java/com/cp/friend/port/MessageNotifications.java#L3); [MatchingStrategy.java:9](../src/main/java/com/cp/friend/strategy/MatchingStrategy.java#L9); [UserInterests.java:4](../src/main/java/com/cp/friend/service/UserInterests.java#L4) | แยกสัญญา storage, แจ้งเตือนข้อความ, matching และความสนใจ ผู้เรียกใช้เฉพาะงานที่เกี่ยวข้อง |
| D — Dependency Inversion | [AuthService.java:25](../src/main/java/com/cp/friend/service/AuthService.java#L25); [UserService.java:23](../src/main/java/com/cp/friend/service/UserService.java#L23); [UserService.java:24](../src/main/java/com/cp/friend/service/UserService.java#L24); [ChatMessageService.java:30](../src/main/java/com/cp/friend/service/ChatMessageService.java#L30); [MatchingServiceImpl.java:36](../src/main/java/com/cp/friend/service/impl/MatchingServiceImpl.java#L36) | AuthService และ UserService รับ StoragePort และ UserInterests, ChatMessageService รับ MessageNotifications และ MatchingServiceImpl รับ MatchingStrategy ผ่าน constructor ทำให้ส่วนที่เรียกใช้งานพึ่งสัญญาของบริการ และใช้ mock แทนบริการเหล่านี้ในการทดสอบได้ |

## ตัวอย่างการทำงานร่วมกันของคลาส

### การส่งข้อความและรูปภาพ

ChatMessageController รับคำขอจากผู้ใช้และส่งต่อให้ ChatMessageService ตรวจสอบสิทธิ์สมาชิกในห้อง จากนั้น MessageContentStrategyResolver เลือก strategy ตามชนิดข้อความ TextContentStrategy ตรวจข้อความ ส่วน ImageContentStrategy ตรวจข้อมูลรูปและส่งต่อการจัดเก็บผ่าน StoragePort โดย StorageTool เป็น implementation ที่ติดต่อบริการจัดเก็บไฟล์ วิธีนี้แยกกฎของเนื้อหาออกจากการรับคำขอและการติดต่อระบบภายนอก

### การแยกบริการข้อความและการคอล

ChatMessageService รับผิดชอบการส่ง อ่าน แก้ไข และลบข้อความ ส่วน [CallSignalingService](../src/main/java/com/cp/friend/service/CallSignalingService.java) ตรวจสอบสมาชิกและส่งต่อคำเชิญคอลกับสัญญาณ WebRTC โดย ChatWsController เลือกเรียกบริการตามชนิดคำขอ การแยกนี้คงปลายทาง WebSocket และรูปแบบข้อมูลเดิมไว้ และทำให้ทดสอบงานคอลแยกจากงานข้อความได้

### การแนะนำผู้ใช้ที่มีความสนใจตรงกัน

MatchingServiceImpl จัดเตรียมผู้ใช้ที่เข้าเงื่อนไขและเรียก MatchingStrategy เพื่อคำนวณความเข้ากันได้ โดย InterestMatchingStrategy รับผิดชอบวิธีคำนวณจากความสนใจร่วมกัน การแยกหน้าที่นี้ช่วยให้ทดสอบขั้นตอนการแนะนำและอัลกอริทึมคำนวณได้เป็นส่วน ๆ

### การทดสอบบริการ

การรับ dependency ผ่าน constructor ทำให้ชุดทดสอบสร้าง service โดยส่ง mock ของ repository, StoragePort, MessageNotifications หรือ MatchingStrategy เข้าไปได้ จึงตรวจสอบกฎการทำงานและการเรียกใช้บริการที่เกี่ยวข้องได้โดยไม่ต้องเรียกบริการภายนอกจริงใน unit test

## Diagram ประกอบ

[Layered Class Diagram](diagrams/class/class-layered.svg) แสดงการแบ่งชั้นและทิศทาง dependency ส่วน [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) แสดงความสัมพันธ์ระหว่าง strategy, คลาสแม่ และคลาสที่ใช้งาน
