# Design Patterns และเหตุผลที่เลือกใช้

[SOLID](solid-analysis.md) · [Design Patterns](design-patterns.md) · [เอกสารการทดสอบ](../test/README.md)

ระบบ CPFriendFinder ใช้รูปแบบการออกแบบเพื่อจัดโครงสร้างแอปพลิเคชัน แยกกฎการทำงาน และลดการผูกกันระหว่างส่วนต่าง ๆ โดยเลือกใช้ Enterprise / Architectural Patterns 6 รายการ และ GoF Behavioral Patterns 3 แบบ ดังนี้

## Enterprise / Architectural Patterns — 6 รายการ

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| Layered Architecture | ลดการผูก HTTP กับ query/ฐานข้อมูล | [ChatRoomController.java:35](../src/main/java/com/cp/friend/controller/ChatRoomController.java#L35) → [ChatRoomService.java:44](../src/main/java/com/cp/friend/service/ChatRoomService.java#L44) → [ChatRoomRepository.java:16](../src/main/java/com/cp/friend/repository/ChatRoomRepository.java#L16) | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| MVC | แยกการเลือก view ออกจาก logic และข้อมูล | [PageController.java:25](../src/main/java/com/cp/friend/controller/PageController.java#L25), templates/home.html, model/User | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Repository | รวมการเข้าถึงฐานข้อมูลและ query | [UserRepository.java:27](../src/main/java/com/cp/friend/repository/UserRepository.java#L27), ChatRoomRepository, MessageRepository | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Service Layer | รวม transaction และ business rules | [FriendRequestService.java:46](../src/main/java/com/cp/friend/service/FriendRequestService.java#L46), ChatRoomService, ChatMessageService, [CallSignalingService](../src/main/java/com/cp/friend/service/CallSignalingService.java) | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| DTO + Mapper | ควบคุม API contract และหลีกเลี่ยงเปิด entity/password hash โดยตรง | [UserMapper.java:13](../src/main/java/com/cp/friend/mapper/UserMapper.java#L13), [InterestMapper.java:13](../src/main/java/com/cp/friend/mapper/InterestMapper.java#L13), dto/request และ dto/response | [Layered Class Diagram](diagrams/class/class-layered.svg) |
| Dependency Injection | เปลี่ยน dependency และ mock ได้โดยไม่ new implementation ใน service | [MatchingServiceImpl.java:36](../src/main/java/com/cp/friend/service/impl/MatchingServiceImpl.java#L36), [UserService.java:23](../src/main/java/com/cp/friend/service/UserService.java#L23) | [Layered Class Diagram](diagrams/class/class-layered.svg) |

## GoF ที่เลือก: Behavioral — 3 แบบในกลุ่มเดียวกัน

| Pattern | ปัญหาที่แก้ | ไฟล์ / คลาสที่ใช้ | Class Diagram ประกอบ |
|---|---|---|---|
| Strategy | ข้อความแต่ละชนิดและ matching algorithm มีกฎต่างกัน | [MessageContentStrategy.java:7](../src/main/java/com/cp/friend/service/strategy/MessageContentStrategy.java#L7), [MessageContentStrategyResolver.java:19](../src/main/java/com/cp/friend/service/strategy/MessageContentStrategyResolver.java#L19), TextContentStrategy, ImageContentStrategy, FileContentStrategy; [MatchingStrategy.java:9](../src/main/java/com/cp/friend/strategy/MatchingStrategy.java#L9), InterestMatchingStrategy | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |
| Observer | เปลี่ยนสถานะคำขอเพื่อนแล้วแจ้งเตือนโดยไม่เรียก NotificationService ตรง | [FriendRequestService.java:66](../src/main/java/com/cp/friend/service/FriendRequestService.java#L66), [NotificationObserver.java:11](../src/main/java/com/cp/friend/observer/NotificationObserver.java#L11), [NotificationService.java:45](../src/main/java/com/cp/friend/service/NotificationService.java#L45), event/FriendRequest*Event | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |
| Template Method | ลำดับ process ต้องเหมือนกัน แต่ validation/transformation ต่างกัน | [AbstractMessageContentStrategy.java:11](../src/main/java/com/cp/friend/service/strategy/AbstractMessageContentStrategy.java#L11), TextContentStrategy, ImageContentStrategy, FileContentStrategy | [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) |

MessageContentStrategyResolver รับ strategy ผ่าน interface และลงทะเบียนตาม supportedType() จึงรองรับ implementation ที่ไม่ได้สืบทอดคลาสแม่ด้วย ส่วน Strategy แยกวิธีประมวลผลตามชนิดข้อความ ส่วน Template Method กำหนดลำดับ normalize → validate → transform ให้คลาสลูกใช้งานร่วมกัน สำหรับ Observer นั้น FriendRequestService ส่ง application event เมื่อคำขอเพื่อนเปลี่ยนสถานะ แล้ว NotificationService รับ event ผ่าน Spring EventListener เพื่อสร้างและส่งการแจ้งเตือนภายในกระบวนการทำงานของแอปพลิเคชัน

นอกจากนี้ StorageTool ทำหน้าที่เชื่อม StoragePort กับบริการจัดเก็บไฟล์ และ ChatRoomDetailResponse.Builder ช่วยสร้างข้อมูลตอบกลับของห้องสนทนา โดย Behavioral Patterns ที่ใช้เป็นหัวข้อหลักในเอกสารนี้ ได้แก่ Strategy, Observer และ Template Method

## Diagram ประกอบ

[Layered Class Diagram](diagrams/class/class-layered.svg) แสดง Controller, Service, Repository, Mapper, Port และ dependency ที่เป็นตัวแทนของแต่ละชั้น ส่วน [Behavioral Class Diagram](diagrams/class/class-behavioral-patterns.svg) แสดง Strategy, Observer และ Template Method พร้อมตำแหน่งคลาสที่ใช้งานจริง

การแบ่งหน้าที่ของคลาสและการพึ่งพา abstraction ที่เกี่ยวข้องกับรูปแบบเหล่านี้อธิบายเพิ่มเติมใน [การวิเคราะห์ SOLID Principles](solid-analysis.md)
