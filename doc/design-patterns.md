# Design Patterns และเหตุผลที่เลือกใช้

[SOLID](solid-analysis.md) · [Design Patterns](design-patterns.md) · [เอกสารการทดสอบ](../test/README.md)

ตรวจจาก `develop` commit `b909af972f5016b20945d23aba9a9537c7ebdbe9` วันที่ 10 ตุลาคม 2026 เอกสารระบุเฉพาะสิ่งที่ตรวจพบใน source snapshot นี้


## Enterprise / Architectural Patterns — 6 รายการ

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| Layered Architecture | ลดการผูก HTTP กับ query/ฐานข้อมูล | [ChatRoomController.java:35](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/controller/ChatRoomController.java#L35) → [ChatRoomService.java:44](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/ChatRoomService.java#L44) → [ChatRoomRepository.java:16](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/repository/ChatRoomRepository.java#L16) | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| MVC | แยกการเลือก view ออกจาก logic และข้อมูล | [PageController.java:25](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/controller/PageController.java#L25), templates/home.html, model/User | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Repository | รวมการเข้าถึงฐานข้อมูลและ query | [UserRepository.java:27](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/repository/UserRepository.java#L27), ChatRoomRepository, MessageRepository | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Service Layer | รวม transaction และ business rules | [FriendRequestService.java:46](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/FriendRequestService.java#L46), ChatRoomService, ChatMessageService | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| DTO + Mapper | ควบคุม API contract และหลีกเลี่ยงเปิด entity/password hash โดยตรง | [UserMapper.java:13](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/mapper/UserMapper.java#L13), [InterestMapper.java:13](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/mapper/InterestMapper.java#L13), dto/request และ dto/response | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Dependency Injection | เปลี่ยน dependency และ mock ได้โดยไม่ new implementation ใน service | [MatchingServiceImpl.java:36](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/impl/MatchingServiceImpl.java#L36), [UserService.java:23](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/UserService.java#L23) | [Layered Class Diagram](diagrams/class/class-layered.svg) |

## GoF ที่เลือก: Behavioral — 3 แบบในกลุ่มเดียวกัน

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| Strategy | ข้อความแต่ละชนิดและ matching algorithm มีกฎต่างกัน | [MessageContentStrategy.java:7](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/strategy/MessageContentStrategy.java#L7), [MessageContentStrategyResolver.java:27](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/strategy/MessageContentStrategyResolver.java#L27), TextContentStrategy, ImageContentStrategy, FileContentStrategy; [MatchingStrategy.java:9](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/strategy/MatchingStrategy.java#L9), InterestMatchingStrategy | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |
| Observer | เปลี่ยนสถานะคำขอเพื่อนแล้วแจ้งเตือนโดยไม่เรียก NotificationService ตรง | [FriendRequestService.java:66](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/FriendRequestService.java#L66), [NotificationObserver.java:11](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/observer/NotificationObserver.java#L11), [NotificationService.java:45](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/NotificationService.java#L45), event/FriendRequest*Event | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |
| Template Method | ลำดับ process ต้องเหมือนกัน แต่ validation/transformation ต่างกัน | [AbstractMessageContentStrategy.java:11](https://github.com/leeLnwCode/CPFriendFinder/blob/b909af972f5016b20945d23aba9a9537c7ebdbe9/src/main/java/com/cp/friend/service/strategy/AbstractMessageContentStrategy.java#L11), TextContentStrategy, ImageContentStrategy, FileContentStrategy | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |

Strategy เลือกวิธีทำงาน ส่วน Template Method ล็อกลำดับภายในวิธีที่ถูกเลือก จึงแก้คนละปัญหา Observer ใช้ Spring application events แบบ synchronous ภายใน process ตามโค้ดปัจจุบัน ไม่ใช่ durable queue

StorageTool ที่ implements StoragePort และ ChatRoomDetailResponse.Builder เป็นตัวอย่างเพิ่มเติม ไม่นำ Adapter/Builder/Factory มารวมแทน Behavioral 3 แบบ

## Diagram ประกอบ

[Layered Class Diagram](diagrams/class/class-layered.svg) แสดง Controller, Service, Repository, Mapper, Port และ dependency ที่เป็นตัวแทนของแต่ละชั้น ส่วน [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) แสดง Strategy, Observer และ Template Method พร้อมตำแหน่งคลาสที่ใช้งานจริง

ภาพอยู่ใน ZIP Diagram แยกชุด โดยแตกไฟล์ที่ราก repository เพื่อให้ลิงก์ด้านบนใช้งานได้ จุดที่ยังควรปรับด้าน DIP/OCP อธิบายใน [SOLID analysis](solid-analysis.md) โดยไม่ได้อ้างว่าโค้ดได้แก้ครบทุกประเด็นแล้ว
