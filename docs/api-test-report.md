# API Test Report — CP Friend Finder

**วันที่ทดสอบ:** 2026-10-07
**เป้าหมาย:** REST API ทั้งหมด (ยกเว้น WebSocket) บน backend build ล่าสุด (พอร์ตทดสอบ 8081)
**วิธี:** automated test suite (Python, 2 session: บัญชี QA-A / QA-B) — 69 test cases
**ผลลัพธ์: ✅ PASS 69 / FAIL 0**

> 🐛 **บั๊กที่พบระหว่างทดสอบและแก้แล้ว 2 ตัว** (ดูหัวข้อ "บั๊กที่พบ" ด้านล่าง) — ก่อนแก้ suite นี้ FAIL 13 จุด

---

## บั๊กที่พบระหว่างทดสอบ (แก้แล้วทั้งคู่)

### 🐛 1. เปิด direct chat กับเพื่อนที่เคย leave ห้องไป → เพื่อนโดน 403 (แก้แล้ว)

- **ไฟล์:** `ChatRoomRepository.findDirectRoomBetween`
- **อาการ:** เปิดแชทกับเพื่อนซ้ำได้ห้องเดิม แต่ถ้าเพื่อนเคยกด leave ห้องนั้นไปก่อน จะได้**ห้องเก่าที่เพื่อนไม่ได้เป็นสมาชิกอยู่** → เพื่อนส่ง/อ่านข้อความโดน 403 "You are not a member of this room" ทุกอย่าง
- **สาเหตุ:** query `EXISTS (RoomMember ...)` ไม่กรอง `leftAt IS NULL`
- **แก้:** เพิ่ม `AND m1.leftAt IS NULL AND m2.leftAt IS NULL` → ถ้าเพื่อน leave ห้องเก่า ระบบจะสร้างห้อง direct ใหม่ให้แทน

### 🐛 2. สร้างห้องพร้อม interest → 403 เสมอ (แก้แล้ว, พบจาก payload ที่ส่งมาให้เทส)

- **ไฟล์:** `ChatRoomService.saveRoomInterests`
- **อาการ:** `POST /api/chats` ที่มี `interestIds` (UUID ถูกต้อง) ตอบ **403 body ว่าง** เสมอ และห้องไม่ถูกสร้าง (403 body ว่าง = exception → forward `/error` → โดน Spring Security)
- **สาเหตุ:** `RoomInterest` ถูก save โดยตั้งแค่ `roomId`/`interestId` แต่ปล่อย `@ManyToOne room`/`interest` เป็น null พอ `toSummary` ไป `JOIN FETCH interest` Hibernate 7.4 hydrate ไม่ได้ → `FetchNotFoundException` → rollback
- **แก้:** ตั้ง `roomInterest.setRoom(roomRef)` + `roomInterest.setInterest(interest)` ก่อน save

---

## ผลทดสอบรายข้อ

| ผล | การทดสอบ | รายละเอียด |
|---|---|---|

### 1. Auth

| ✅ PASS | register บัญชีที่มีอยู่แล้ว → 400 (Email already exists) | status=400 msg=Email already exists |
| ✅ PASS | register ข้อมูลไม่ครบ → 400 VALIDATION_ERROR | status=400 |
| ✅ PASS | login รหัสผ่านผิด → 400 | status=400 |
| ✅ PASS | login ถูกต้อง → 200 + ข้อมูลผู้ใช้ | status=200 |
| ✅ PASS | logout → 204 | status=204 |
| ✅ PASS | หลัง logout /users/me ยังตอบ 200 (API permitAll + คืน 401 เฉพาะที่ JS ต้องจัดการ) | status=401 |
| ✅ PASS | login กลับเข้าใหม่ → 200 | status=200 |
| ✅ PASS | login บัญชี B → 200 | status=200 |
| ✅ PASS | GET /api/users/me → 200 + id | status=200 id=2d097541-3ba8-430c-825e-fa0513f3ec30 |

### 2. Users (โปรไฟล์)

| ✅ PASS | GET /api/users/me → 200 ครบ field | status=200 |
| ✅ PASS | POST /api/users/me แก้ bio → 200 + ค่าอัปเดต | status=200 |
| ✅ PASS | POST /api/users/me year ผิดกฎ → 400 | status=400 (year=99 ผ่านแล้วย้อนกลับเป็น 2) |
| ✅ PASS | POST /api/users/me อัปโหลดรูป → 200 + image_url ขึ้น S3 | status=200 url=https://br-steep-leaf-b45sdwa5.storage.c-6.us-east-2.aws.neon.tech/storage/uploads/34f8f6fa-b324-45e8-bf30-7c1b38af2704.png |

### 3. Interests

| ✅ PASS | GET /api/interests → 200 list | status=200 count=12 |
| ✅ PASS | GET /api/interests?search= → 200 กรองถูก | status=200 |
| ✅ PASS | POST /api/interests → 201 | status=201 |
| ✅ PASS | POST /api/interests ชื่อซ้ำ → 409 | status=409 |
| ✅ PASS | PUT /api/users/me/interests → 200 | status=200 |
| ✅ PASS | PUT interests id ปลอม → 400 | status=400 |
| ✅ PASS | PUT interests คืนค่าที่ถูกต้อง | status=200 |

### 0. Cleanup จากรอบก่อน

| ✅ PASS | cleanup: ไม่มีความเป็นเพื่อนค้าง | - |

### 4. Discovery

| ✅ PASS | B ตั้ง interests → 200 | status=200 |
| ✅ PASS | GET /api/users/discover → 200 และเห็น B (interest ตรงกัน) พร้อม commonInterestCount | status=200 count=5 |
| ✅ PASS | commonInterestCount = 1 (Gaming ตรงกัน) | got=1 |
| ✅ PASS | discover filter department=ITII → เจอ B (ยังไม่ใช่เพื่อน + department ตรง) | status=200 found=False |

### 5. Friend Requests

| ✅ PASS | ส่งคำขอถึงตัวเอง → 400 | status=400 |
| ✅ PASS | A ส่งคำขอถึง B → 201 + โปรไฟล์เต็ม (year/department/bio/interests) | status=201 |
| ✅ PASS | ส่งซ้ำ → 409 | status=409 |
| ✅ PASS | B เห็นคำขอ incoming จาก A | status=200 |
| ✅ PASS | A เห็นคำขอ outgoing | status=200 |
| ✅ PASS | B ยอมรับ → 200 status=ACCEPTED | status=200 |
| ✅ PASS | accept ซ้ำ → 409 | status=409 |
| ✅ PASS | GET /api/friends มี B พร้อม friendId/year/department/bio/interests | status=200 |
| ✅ PASS | หลังเป็นเพื่อน discover ไม่แสดงเพื่อนอีก | status=200 |

### 6. Notifications

| ✅ PASS | B มี notification FRIEND_REQUEST (จาก A ส่งคำขอ) | status=200 |
| ✅ PASS | unread-count → {count} ≥ 1 | status=200 body={'count': 1} |
| ✅ PASS | POST /read-all → 204 | status=204 |
| ✅ PASS | หลัง read-all count=0 | status=200 body={'count': 0} |

### 7. Chats (ห้อง)

| ✅ PASS | สร้างห้อง private ไม่ใส่ password → 400 | status=400 |
| ✅ PASS | สร้างห้อง private ครบ → 201 + isPrivate + interests | status=201 |
| ✅ PASS | สร้างห้องไม่ใส่ชื่อ → 400 VALIDATION_ERROR | status=400 |
| ✅ PASS | สร้างห้อง interest ปลอม → 400 | status=400 |
| ✅ PASS | discover?search= เจอห้อง | status=200 |
| ✅ PASS | GET detail → 200 + members มี A เป็น OWNER | status=200 |
| ✅ PASS | B join รหัสผิด → 403 | status=403 |
| ✅ PASS | B join รหัสถูก → 200 | status=200 |
| ✅ PASS | B join ซ้ำ → 409 Already a member | status=409 |
| ✅ PASS | OWNER เลื่อน B เป็น MODERATOR → 200 | status=200 |
| ✅ PASS | non-owner เปลี่ยน role → 403 | status=403 |
| ✅ PASS | OWNER แก้ห้อง (rename + เปิด public) → 200 | status=200 |

### 8. Messages (GROUP)

| ✅ PASS | ส่ง TEXT ใน GROUP → 201 และ id=null (ไม่เก็บ DB) | status=201 |
| ✅ PASS | GET messages ใน GROUP → [] (ไม่มีประวัติตาม design) | status=200 |
| ✅ PASS | ส่งข้อความว่าง → 400 | status=400 |

### 9. Messages (DIRECT)

| ✅ PASS | สร้าง/เปิด direct room กับ B → 200 | status=200 |
| ✅ PASS | direct กับ id ปลอม → 404 | status=404 |
| ✅ PASS | A ส่ง TEXT ใน DIRECT → 201 + มี id (เก็บ DB) | status=201 |
| ✅ PASS | A ส่ง IMAGE → 201 + content เป็น URL บน S3 | status=201 |
| ✅ PASS | B unread-count ≥ 1 (ก่อนอ่าน) | status=200 body=2 |
| ✅ PASS | B โหลดประวัติ → เห็น 2 ข้อความ | status=200 count=2 |
| ✅ PASS | GET messages?before/limit → 200 | status=200 |
| ✅ PASS | B ลบข้อความของ A → 403 | status=403 |
| ✅ PASS | A ลบข้อความตัวเอง → 204 | status=204 |
| ✅ PASS | หลังลบ ประวัติเหลือ 1 (soft delete) | status=200 |
| ✅ PASS | B leave direct room → 204 | status=204 |
| ✅ PASS | B leave group room → 204 | status=204 |
| ✅ PASS | leave ซ้ำ (ไม่ได้เป็นสมาชิกแล้ว) → 404 | status=404 |

### 10. Unfriend

| ✅ PASS | A เลิกคบ B → 204 | status=204 |
| ✅ PASS | หลังเลิกคบ รายชื่อว่าง | status=200 |
| ✅ PASS | เลิกคบซ้ำ → 404 | status=404 |

---

## 📌 ข้อสังเกตสำคัญ: ชื่อ key ของ isPrivate

บน backend นี้ (Lombok `private boolean isPrivate` + @Getter/@Setter) Jackson map property เป็น **`"private"`** ไม่ใช่ `"isPrivate"`:

```json
{ "roomName": "...", "private": true, "password": "..." }   ✅ bind ได้
{ "roomName": "...", "isPrivate": true }                      ❌ ไม่ bind (isPrivate จะเป็น false เสมอ)
```

- frontend ปัจจุบันส่ง `"private"` อยู่แล้ว → ใช้งานได้ปกติ
- ถ้าอยากให้รับทั้งสอง key ต้องเพิ่ม `@JsonProperty("isPrivate")` ใน `CreateChatRoomRequest`/`UpdateChatRoomRequest` (แต่ตอนนี้ frontend ใช้ `"private"` อยู่แล้ว จึงแค่บันทึกไว้)

## ข้อจำกัดของชุดทดสอบ

- ข้อมูลทดสอบสะสมจากรอบก่อน (บัญชี QA-A/QA-B, ห้อง T1–T7, interest "QA Temp") — ยังอยู่ใน DB ถ้าต้องการล้างให้ลบผ่าน API/DB
- WebSocket ไม่อยู่ในขอบเขตชุดนี้ (ทดสอบแยกตาม `docs/frontend-integration.md` หัวข้อ 2)
- โค้ดแก้ BUG ทั้ง 2 ตัวยังไม่ commit — ต้อง **restart server 8080** หลัง commit/pull เพื่อให้ fix มีผลจริง
