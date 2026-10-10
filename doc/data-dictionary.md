# Data Dictionary — CP Friend Finder

พจนานุกรมข้อมูลอธิบายตารางและคอลัมน์ทั้งหมดของฐานข้อมูล PostgreSQL 14 ตาราง
อ้างอิงจาก `src/main/resources/schema.sql` (สร้างจาก Hibernate mappings ของ Entity ใน `com.cp.friend.model`)

- **Key** — PK = Primary Key, FK = Foreign Key, UK = Unique, PK,FK = คีย์ร่วมของตารางเชื่อม
- **Null** — NO = ห้ามเป็นค่าว่าง (NOT NULL), YES = เป็นค่าว่างได้

---

## ตาราง: users (บัญชีผู้ใช้)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสประจำตัวผู้ใช้ |
| email | VARCHAR(255) | UK | NO | - | อีเมลสำหรับเข้าสู่ระบบ (ห้ามซ้ำ) |
| password_hash | VARCHAR(255) | - | NO | - | รหัสผ่านที่เข้ารหัสแล้ว (BCrypt) |
| firstname | VARCHAR(255) | - | YES | - | ชื่อจริง |
| lastname | VARCHAR(255) | - | YES | - | นามสกุล |
| date_of_birth | DATE | - | YES | - | วันเกิด |
| year | SMALLINT | - | YES | - | ชั้นปีที่กำลังศึกษา |
| department | VARCHAR(255) | - | YES | - | สาขาวิชา |
| bio | VARCHAR(500) | - | YES | - | ข้อความแนะนำตัว |
| image_url | VARCHAR(255) | - | YES | - | ที่อยู่รูปภาพโปรไฟล์ |
| status | VARCHAR(255) | - | NO | - | สถานะบัญชี: ACTIVE / BLOCKED |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่สร้างบัญชี |
| updated_at | TIMESTAMPTZ | - | NO | - | วันที่แก้ไขล่าสุด |
| last_seen_at | TIMESTAMPTZ | - | YES | - | เวลาที่ใช้งานล่าสุด |

## ตาราง: user_profiles (โปรไฟล์เพิ่มเติมของผู้ใช้)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| user_id | UUID | PK, FK | NO | - | เจ้าของโปรไฟล์ (ความสัมพันธ์ 1:1 กับ users) |
| bio | VARCHAR(500) | - | YES | - | ข้อความแนะนำตัวเพิ่มเติม |

## ตาราง: user_gallery_photos (แกลเลอรีรูปภาพของผู้ใช้)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| user_id | UUID | PK, FK | NO | - | เจ้าของรูปภาพ (ผู้ใช้หนึ่งคนมีได้สูงสุด 5 รูป) |
| photo_order | INTEGER | PK | NO | 0 | ลำดับรูปในแกลเลอรี (0–4) |
| image_url | VARCHAR(2048) | - | NO | - | ที่อยู่รูปภาพบนระบบจัดเก็บวัตถุ (S3) |

## ตาราง: interests (ความสนใจ)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสความสนใจ |
| name | VARCHAR(100) | UK | NO | - | ชื่อความสนใจ (ห้ามซ้ำ) |
| is_active | BOOLEAN | - | NO | - | สถานะเปิดใช้งาน |

## ตาราง: user_interests (ความสนใจของผู้ใช้ — ตารางเชื่อม)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| user_id | UUID | PK, FK | NO | - | ผู้ใช้ (Many-to-Many กับ interests) |
| interest_id | UUID | PK, FK | NO | - | ความสนใจที่เลือก |

## ตาราง: friend_requests (คำขอเป็นเพื่อน)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสคำขอเป็นเพื่อน |
| sender_id | UUID | FK | NO | - | ผู้ส่งคำขอ |
| receiver_id | UUID | FK | NO | - | ผู้รับคำขอ |
| status | VARCHAR(255) | - | NO | - | สถานะ: PENDING / ACCEPTED / DECLINED |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่ส่งคำขอ |
| responded_at | TIMESTAMPTZ | - | YES | - | วันที่ตอบรับ/ปฏิเสธ |

## ตาราง: friendships (ความสัมพันธ์เพื่อน)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสความสัมพันธ์ |
| user_id | UUID | FK | NO | - | เจ้าของรายชื่อเพื่อน |
| friend_id | UUID | FK | NO | - | เพื่อน (เก็บสองทิศทาง: A→B และ B→A) |
| nickname | VARCHAR(100) | - | YES | - | ชื่อเล่นที่ตั้งให้เพื่อน |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่เป็นเพื่อนกัน |

## ตาราง: notifications (การแจ้งเตือน)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสการแจ้งเตือน |
| user_id | UUID | FK | NO | - | ผู้รับการแจ้งเตือน |
| actor_user_id | UUID | FK | YES | - | ผู้ก่อให้เกิดเหตุการณ์ (เช่น ผู้ส่งคำขอ) |
| room_id | UUID | FK | YES | - | ห้องแชทที่เกี่ยวข้อง (กรณีแจ้งเตือนข้อความ) |
| friend_request_id | UUID | FK | YES | - | คำขอเป็นเพื่อนที่เกี่ยวข้อง |
| type | VARCHAR(30) | - | NO | - | ประเภท: FRIEND_REQUEST / NEW_MESSAGE / ROOM_INVITE / SYSTEM |
| title | VARCHAR(255) | - | YES | - | หัวข้อการแจ้งเตือน |
| message | TEXT | - | YES | - | เนื้อความการแจ้งเตือน |
| is_read | BOOLEAN | - | NO | - | อ่านแล้วหรือยัง |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่สร้างการแจ้งเตือน |

## ตาราง: chat_rooms (ห้องแชท)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสห้องแชท |
| room_name | VARCHAR(100) | - | NO | - | ชื่อห้อง |
| description | VARCHAR(500) | - | YES | - | คำอธิบายห้อง |
| room_type | VARCHAR(255) | - | NO | - | ประเภทห้อง: GROUP / DIRECT |
| is_private | BOOLEAN | - | NO | - | true = ห้องส่วนตัว (ต้องใช้รหัสผ่าน) |
| password_hash | VARCHAR(255) | - | YES | - | รหัสผ่านเข้าห้อง (เข้ารหัสแล้ว) |
| max_members | SMALLINT | - | NO | - | จำนวนสมาชิกสูงสุด (ไม่เกิน 10) |
| target_year | SMALLINT | - | YES | - | ชั้นปีเป้าหมายของห้อง |
| created_by | UUID | FK | YES | - | ผู้สร้างห้อง |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่สร้างห้อง |
| updated_at | TIMESTAMPTZ | - | NO | - | วันที่แก้ไขล่าสุด |
| deleted_at | TIMESTAMPTZ | - | YES | - | วันที่ลบห้อง (soft delete, null = ยังใช้งาน) |

## ตาราง: room_members (สมาชิกห้องแชท)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสการเป็นสมาชิก |
| room_id | UUID | FK | NO | - | ห้องแชท (Many-to-Many กับ users) |
| user_id | UUID | FK | NO | - | สมาชิก |
| role | VARCHAR(255) | - | NO | - | บทบาท: OWNER / MODERATOR / MEMBER |
| joined_at | TIMESTAMPTZ | - | NO | - | วันที่เข้าร่วมห้อง |
| last_read_at | TIMESTAMPTZ | - | YES | - | เวลาอ่านข้อความล่าสุด |
| left_at | TIMESTAMPTZ | - | YES | - | วันที่ออกจากห้อง (null = ยังเป็นสมาชิกอยู่) |

## ตาราง: room_interests (ความสนใจของห้อง — ตารางเชื่อม)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| room_id | UUID | PK, FK | NO | - | ห้องแชท |
| interest_id | UUID | PK, FK | NO | - | ความสนใจของห้อง |

## ตาราง: messages (ข้อความในห้องแชท)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสข้อความ |
| room_id | UUID | FK | NO | - | ห้องที่ส่งข้อความ |
| sender_id | UUID | FK | NO | - | ผู้ส่งข้อความ |
| content | TEXT | - | NO | - | เนื้อหาข้อความ |
| message_type | VARCHAR(255) | - | NO | - | ประเภท: TEXT / IMAGE / FILE |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่ส่ง |
| updated_at | TIMESTAMPTZ | - | NO | - | วันที่แก้ไขล่าสุด |
| deleted_at | TIMESTAMPTZ | - | YES | - | วันที่ลบ (soft delete) |

## ตาราง: random_chat_sessions (เซสชันสุ่มคุย)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสเซสชัน |
| user1_id | UUID | FK | NO | - | ผู้เข้าคิวคนแรก |
| user2_id | UUID | FK | YES | - | คู่สนทนา (จับคู่สำเร็จเมื่อมีค่า) |
| status | VARCHAR(255) | - | NO | - | สถานะ: WAITING / MATCHED / ENDED / CANCELLED |
| started_at | TIMESTAMPTZ | - | NO | - | เวลาเริ่มเข้าคิว |
| matched_at | TIMESTAMPTZ | - | YES | - | เวลาที่จับคู่สำเร็จ |
| ended_at | TIMESTAMPTZ | - | YES | - | เวลาจบสนทนา |
| ended_by_user_id | UUID | FK | YES | - | ผู้กดจบสนทนา |

## ตาราง: random_chat_messages (ข้อความในระบบสุ่มคุย)

| คอลัมน์ | ชนิดข้อมูล | Key | Null | Default | คำอธิบาย |
|---|---|---|---|---|---|
| id | UUID | PK | NO | - | รหัสข้อความ |
| session_id | UUID | FK | NO | - | เซสชันสุ่มคุยที่ข้อความอยู่ |
| sender_id | UUID | FK | NO | - | ผู้ส่งข้อความ |
| content | TEXT | - | NO | - | เนื้อหาข้อความ |
| message_type | VARCHAR(255) | - | NO | - | ประเภท: TEXT / IMAGE / FILE |
| created_at | TIMESTAMPTZ | - | NO | - | วันที่ส่ง |
| updated_at | TIMESTAMPTZ | - | NO | - | วันที่แก้ไขล่าสุด |
| deleted_at | TIMESTAMPTZ | - | YES | - | วันที่ลบ (soft delete) |

---

## ดัชนี (Indexes)

| ดัชนี | ตาราง | คอลัมน์ | ใช้งาน |
|---|---|---|---|
| ix_room_members_room_active | room_members | (room_id, left_at) | หาสมาชิกที่ยังอยู่ในห้อง |
| ix_room_members_user_active | room_members | (user_id, left_at) | หาห้องที่ผู้ใช้ยังเป็นสมาชิก |
| ix_messages_room_created | messages | (room_id, created_at DESC) | อ่านประวัติข้อความของห้องเรียงตามเวลา |
| ix_notifications_user_read | notifications | (user_id, is_read, created_at DESC) | โหลดการแจ้งเตือนของผู้ใช้ |
| ix_friend_requests_receiver_status | friend_requests | (receiver_id, status) | รายการคำขอที่รอผู้รับตอบ |
| ix_friend_requests_sender_status | friend_requests | (sender_id, status) | รายการคำขอที่ผู้ส่งส่งไป |
| ix_friendships_user / ix_friendships_friend | friendships | (user_id) / (friend_id) | ค้นหารายชื่อเพื่อนสองทิศทาง |
| ix_chat_rooms_discovery | chat_rooms | (room_type, deleted_at, created_at DESC) | ค้นหา/แสดงรายการห้อง |
| ix_user_interests_interest | user_interests | (interest_id) | หาผู้ใช้ที่มีความสนใจเดียวกัน |

## ความสัมพันธ์หลัก (Relationships)

- `users` 1 : 1 `user_profiles`, `users` 1 : 0..5 `user_gallery_photos`
- `users` N : M `interests` ผ่าน `user_interests`
- `users` 1 : N `chat_rooms` (created_by) และ N : M ผ่าน `room_members`
- `chat_rooms` N : M `interests` ผ่าน `room_interests`
- `users` 1 : N `messages`, `chat_rooms` 1 : N `messages`
- `users` (sender/receiver) 1 : N `friend_requests` → ยอมรับแล้วเกิด `friendships`
- `notifications` อ้างอิง `users`, `chat_rooms`, `friend_requests`
- `random_chat_sessions` (user1 / user2 / ended_by) → `users`, 1 : N `random_chat_messages`
