# Frontend Integration Guide — CP Friend Finder

> เอกสารสำหรับทีม Frontend: สัญญาการเชื่อมต่อ (REST + WebSocket + WebRTC) ทั้งหมดของ backend
> backend ตัวนี้เป็น API ล้วน — หน้าเว็บทั้งหมดทำโดยทีม frontend
> (PageController คาดหวัง view ชื่อ: `login`, `register`, `home`, `chat`, `friends`, `profile` — ใส่ template ของทีมได้เลย)

---

## 0. พื้นฐานที่ต้องรู้

- **Base URL:** `http://localhost:8080` (หรือ tunnel HTTPS ตอนทดสอบมือถือ)
- **Auth:** session-based — login ครั้งเดียว browser แนบ cookie `JSESSIONID` เอง
  - ทุก `fetch` ใส่ `credentials: 'same-origin'` / WebSocket handshake ใช้ cookie เดียวกัน
  - ไม่มี token / Authorization header
- **Error format** ทุก endpoint:

```json
{ "success": false, "code": "404 NOT_FOUND", "message": "Room not found", "errors": null }
```

- validation ไม่ผ่าน → `code: "VALIDATION_ERROR"`, `errors: [{field, message}]`
- **401 = ยัง login / session หมด → พาผู้ใช้ไปหน้า login**

### Status codes ที่ใช้

| Status | ความหมาย |
|---|---|
| 200 / 201 / 204 | สำเร็จ / สร้างใหม่ / สำเร็จไม่มี body |
| 400 | validation หรือค่าไม่ถูกต้อง |
| 401 | ยังไม่ login |
| 403 | ไม่มีสิทธิ์ (ไม่ใช่สมาชิกห้อง, รหัสห้องผิด, ไม่ใช่ OWNER) |
| 404 | ไม่พบ resource |
| 409 | ขัดแย้ง (เป็นเพื่อนแล้ว, request ซ้ำ, ห้องเต็ม, เป็นสมาชิกอยู่แล้ว) |

### ⚠️ Gotcha: ชื่อ field ใน response

| Endpoint | field ที่ต้องใช้ | ไม่ใช่ |
|---|---|---|
| `GET /api/friends` | **`friendId`**, `friendsSince` | ~~`id`~~ / ~~`friendSince`~~ |
| `GET /api/chats` (summary) | `id`, `unreadCount`, `roomType`, `isPrivate` | — |
| ทุก response | เขียนตาม record ใน `dto/response/` เสมอ | — |

---

## 1. REST API

### 1.1 Auth

```js
POST /api/auth/register   // 201 — body: {email*, password*(≥8), firstname?, lastname?,
                          //         imageBase64?, dateOfBirth? "YYYY-MM-DD", year?, department?}
POST /api/auth/login      // 200 — {email*, password*} → สร้าง session → {id, email, firstname, lastname, status, image_url}
POST /api/auth/logout     // 204
```

### 1.2 โปรไฟล์ตัวเอง (หน้า /profile)

```js
GET  /api/users/me        // → { id, email, firstname, lastname, bio, dateOfBirth, year,
                          //      department, image_url, status, updatedAt }
POST /api/users/me        // แก้โปรไฟล์ — ส่งเฉพาะ field ที่แก้; ส่ง "" = ล้างค่า
                          // imageBase64: "data:image/png;base64,..." (≤~3MB) → ขึ้น S3
                          // → รูปแบบเดียวกับ GET
```

### 1.3 Interests (หน้า /profile + ใช้กรองทั่วระบบ)

```js
GET  /api/interests                 // ทั้งหมด → [{id, name, isActive}]
GET  /api/interests?search=xxx      // ค้นหา
POST /api/interests                 // {name} → 201 (409 ถ้าชื่อซ้ำ)
GET  /api/users/me/interests        // interest ของฉัน
PUT  /api/users/me/interests        // แทนที่ทั้งหมด: {interestIds: ["<uuid>", ...]} (ส่ง [] = ล้าง)
```

### 1.4 เพื่อน + Friend Request

```js
GET    /api/friends                      // → [{friendId, firstname, lastname, imageUrl, year, department, friendsSince}]
DELETE /api/friends/{friendId}           // เลิกคบ → 204

POST   /api/friend-requests              // {receiverId} → 201 | 409 ถ้าเป็นเพื่อนแล้ว/ส่งแล้ว
GET    /api/friend-requests/incoming     // คำขอที่ได้รับ → [{id, userId, firstname, lastname, imageUrl, status, createdAt}]
GET    /api/friend-requests/outgoing     // คำขอที่ส่งไป
POST   /api/friend-requests/{id}/accept  // → 200
POST   /api/friend-requests/{id}/decline // → 200
```

> เมื่อมีคนส่งคำขอ / กด accept — **ฝั่งตรงข้ามได้ notification push ทันที** ผ่าน WebSocket (ดู หัวข้อ 2)

### 1.5 Discovery (แนะนำเพื่อนจาก interest ตรงกัน)

```js
GET /api/users/discover?department=&year=   // ทั้งสอง filter optional
→ [{ id, firstname, lastname, imageUrl, commonInterestCount }]  // เรียงจากเยอะ→น้อย
```

### 1.6 Notifications

```js
GET  /api/notifications?unread=true&page=0&size=20
→ [{ id, type, title, message, isRead, createdAt, friendRequestId, roomId,
     actor: { id, firstname, lastname, imageUrl } }]

GET  /api/notifications/unread-count      → { "count": 3 }
POST /api/notifications/{id}/read        → 204
POST /api/notifications/read-all         → 204
```

### 1.7 ห้องแชท

```js
GET  /api/chats            // ห้องของฉัน เรียงตาม activity ล่าสุด (DIRECT อยู่บน = "แชทล่าสุด")
→ [{ id, roomName, roomType: "GROUP"|"DIRECT", isPrivate, maxMembers,
     memberCount, unreadCount, interests: [{id, name}], createdAt }]

POST /api/chats             // สร้างห้องกลุ่ม → 201
{ roomName*, description?, interestIds?, maxMembers?, isPrivate?, password? }  // password บังคับเมื่อ isPrivate

GET  /api/chats/discover?search=&interestId=&interestId=&page=0&size=20
                            // ห้อง GROUP สาธารณะ — interestId ใส่ซ้ำได้

GET  /api/chats/{roomId}    // detail → + members: [{userId, firstname, lastname, imageUrl, role}]

POST /api/chats/{roomId}/join    // เข้าห้อง — private ส่ง {"password"} | 403 ผิด / 409 เต็ม-ซ้ำ
POST /api/chats/{roomId}/leave   // ออกจากห้อง → 204

POST /api/chats/direct/{friendId}  // ห้องแชทส่วนตัวกับเพื่อน (ได้ห้องเดิมถ้ามีอยู่แล้ว; ต้องเป็นเพื่อนกัน)
                                   // → ห้องแบบ summary (roomName ใน DB เป็น "DIRECT" — แสดงชื่อเพื่อนแทน)

PUT  /api/chats/{roomId}              // แก้ห้อง (OWNER/MODERATOR): roomName/description/maxMembers/
                                      // isPrivate/password/interestIds — ส่งเฉพาะ field ที่แก้
PUT  /api/chats/{roomId}/members/{userId}/role   // {role: "MODERATOR"|"MEMBER"} — OWNER เท่านั้น
```

### 1.8 ข้อความ

```js
GET    /api/chats/{roomId}/messages?before=2026-01-01T00:00:00Z&limit=50
       // เก่า→ใหม่ | before = ISO instant cursor สำหรับ scroll ย้อนหลัง
       // **ห้อง GROUP คืน [] เสมอ (ไม่เก็บประวัติ)** — มีแค่ DIRECT
       // ⚠️ การเรียก method นี้ = mark อ่านแล้ว (unreadCount กลายเป็น 0)

POST   /api/chats/{roomId}/messages     // ส่งข้อความทาง REST → 201 (ใช้บังคับกับรูป!)
{ "content": "<ข้อความ หรือ base64/dataURL>", "messageType": "IMAGE" }   // TEXT ไม่ต้องส่ง messageType
       // IMAGE → server อัปโหลด S3 แล้ว content ที่ broadcast กลับเป็น URL
       // คนอื่นในห้องได้รับทันทีทาง /topic/rooms/{roomId}

GET    /api/chats/{roomId}/messages/unread-count   → จำนวน
DELETE /api/chats/{roomId}/messages/{messageId}    // ลบข้อความตัวเอง (DIRECT) → 204
```

---

## 2. WebSocket (STOMP — native WebSocket ไม่ใช้ SockJS)

```html
<script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7/bundles/stomp.umd.min.js"></script>
```

```js
const stomp = new StompJs.Client({
  brokerURL: `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}/ws`,
  reconnectDelay: 5000,
});
stomp.onConnect = () => { /* เริ่ม subscribe ที่นี่เท่านั้น */ };
stomp.onStompError = (f) => console.error(f.headers['message']); // เช่น "You are not a member of this room"
stomp.activate();
```

**กฎสำคัญ:**
- ต้อง login ก่อน — handshake ใช้ session cookie ระบุตัวตน (server แปะ `fromUserId` ให้เอง client ปลอมไม่ได้)
- **publish ได้เฉพาะหลัง `onConnect`** — stompjs throw ถ้า publish ตอนยังไม่ต่อ
- **อย่าใช้ SockJS** — server เปิด native WebSocket เท่านั้น (และ SockJS ทำให้เกิด violation log)
- ออกจากหน้า: cleanup ใน `pagehide` (ห้ามใช้ `unload`/`beforeunload` — Chrome บล็อก)

### ตาราง destination ทั้งหมด

| ทิศทาง | Destination | ใช้ทำอะไร |
|---|---|---|
| ส่ง | `/app/rooms/{roomId}/messages` | ข้อความ TEXT `{content}` |
| รับ | `/topic/rooms/{roomId}` | ข้อความใหม่ของห้อง (`ChatMessageResponse`) |
| ส่ง | `/app/rooms/{roomId}/call` | WebRTC signaling `{type, targetUserId?, payload}` |
| รับ | `/topic/rooms/{roomId}/call` | สัญญาณคอลทั้งหมด (broadcast — OFFER/ANSWER/ICE มี `toUserId` ให้กรองเอง) |
| ส่ง | `/app/call` | สายเรียก `{type: INVITE\|ACCEPT\|DECLINE\|CANCEL, toUserId, roomId}` |
| รับ | `/topic/call/{myUserId}` | สายเรียกเข้าของตัวเอง — **subscribe ทุกหน้า** |
| รับ | `/topic/notifications/{myUserId}` | notification push — **subscribe ทุกหน้า** (badge/toast) |

---

## 3. ระบบสายเรียกเข้า (Call Ring)

Modal รับสายต้องทำงาน**ทุกหน้า** — จึงแยกเป็น JS ตัวกลางที่ include ทุกหน้า แล้ว subscribe `/topic/call/{myUserId}` + `/topic/notifications/{myUserId}` ตั้งแต่โหลดหน้า

**Flow:**

```
ผู้โทร (อยู่หน้าแชท/หน้าเพื่อน)          ผู้รับ (เปิดหน้าไหนก็ได้)
─────────────────────────            ─────────────────────────
กดปุ่ม 📹
publish /app/call {INVITE, toUserId, roomId}  ──────▶  modal: โปรไฟล์ผู้โทร + [รับ][ปฏิเสธ]
overlay "กำลังโทร..." (ยกเลิกได้, 45 วิ auto)        │
                                          กดรับ ▼
                            publish {ACCEPT} ────▶ ได้รับ → navigate ไป /chat?roomId=X&join=1
ได้รับ ACCEPT → navigate /chat?roomId=X&join=1  ←────┘
        ทั้งคู่ auto เริ่มคอล → เชื่อม WebRTC (ดู หัวข้อ 4)
```

- ปฏิเสธ → publish `{DECLINE}` → ฝั่งโทร toast "ถูกปฏิเสธสาย"
- ยกเลิก → publish `{CANCEL}` → ฝั่งรับปิด modal
- ผู้รับกดรับตอนเปิดหน้าแชทห้องนั้นอยู่แล้ว → เริ่มคอลเลย ไม่ต้อง navigate

**สัญญาณที่ได้รับบน `/topic/call/{me.id}`:**

```js
{ type: "INVITE"|"ACCEPT"|"DECLINE"|"CANCEL",
  fromUserId, toUserId, roomId, fromName, fromImage }   // fromName/fromImage มาจาก server (ใช้โชว์ modal ได้เลย)
```

---

## 4. วิดีโอคอล WebRTC (mesh ต่อห้อง — ใช้ได้ทั้ง GROUP + DIRECT)

Backend เป็น **signaling relay เท่านั้น** — media วิ่ง P2P ระหว่าง browser

### 4.1 ICE servers (คือ config ของทีม frontend)

```js
const ICE_CONFIG = { iceServers: [
  { urls: "stun:stun.relay.metered.ca:80" },
  { urls: "turn:asia-east.relay.metered.ca:80",      username: "<จากทีม>", credential: "<จากทีม>" },
  { urls: "turn:asia-east.relay.metered.ca:80?transport=tcp", ... },
  { urls: "turn:asia-east.relay.metered.ca:443", ... },
  { urls: "turns:asia-east.relay.metered.ca:443?transport=tcp", ... },
]};
// ⚠️ credential จริงไม่ควร commit — เก็บนอก git / inject ตอน build
```

### 4.2 สัญญาณบน `/topic/rooms/{roomId}/call`

```js
{ type: "JOIN"|"LEAVE", fromUserId, toUserId: null, payload: null }   // broadcast ทั้งห้อง
{ type: "OFFER"|"ANSWER"|"ICE", fromUserId, toUserId, payload }        // toUserId = ผู้รับเท่านั้นที่ประมวลผล
```

### 4.3 Perfect Negotiation (บังคับ — ไม่งั้น offer ชนกันแล้วคอลไม่เชื่อม)

- `polite = myUserId < otherUserId` (เทียบ string) — ฝั่ง polite ยอม rollback เมื่อ offer ชน, ฝั่ง impolite ข้าม offer ที่ชน
- ได้รับ OFFER → `setRemoteDescription(offer)` → **ต้องสร้าง answer เอง**: `await pc.setLocalDescription()` (no-arg) → ส่ง `ANSWER` กลับ — **อย่ารอ `onnegotiationneeded` มันไม่ fire หลังรับ offer** (บั๊กที่เคยทำให้ทั้งระบบไม่เชื่อม)
- ICE candidate ที่มาก่อน `remoteDescription` → buffer ไว้ เติมหลัง setRemoteDescription
- ออกจากคอล → ส่ง `LEAVE` + ปิด pc; ฝั่งที่เหลือจับ `disconnected` + grace 5s แล้วเอา tile ออก (กันวิดีโอค้าง)
- cleanup ตอนออกจากหน้าใช้ event `pagehide`

---

## 5. การเชื่อมต่อรายหน้า

### 📄 `/login` — `POST /api/auth/login` → redirect `/home`

### 📄 `/register` — `POST /api/auth/register` (รูป base64 optional) → ไป `/login`

### 📄 `/home`
- `GET /api/chats` — รายการห้องของฉัน (badge = `unreadCount`)
- `GET /api/chats/discover?search=&interestId=` — ค้นหาห้อง; `POST /api/chats/{id}/join` → เข้า `/chat?roomId=`
- `POST /api/chats` — สร้างห้องกลุ่ม (เลือก interestIds ได้)
- กระดิ่ง: `GET /api/notifications/unread-count` + subscribe `/topic/notifications/{me.id}` (push real-time) + ปุ่มอ่านทั้งหมด
- **include ตัวกลางรับสาย** (หัวข้อ 3) และ subscribe `/topic/call/{me.id}`

### 📄 `/chat?roomId=...` (หน้าคุยเต็ม + วิดีโอ)
- `GET /api/chats/{roomId}` — detail/สมาชิก; ห้อง DIRECT แสดงชื่อเพื่อนแทน roomName
- subscribe `/topic/rooms/{roomId}` + ส่งข้อความ `/app/rooms/{roomId}/messages`
- ประวัติ: `GET .../messages` เฉพาะ DIRECT (GROUP ไม่มีประวัติ — แสดงหมายเหตุให้ผู้ใช้)
- รูป: `POST .../messages` REST (ห้ามส่ง base64 ผ่าน WS — frame ใหญ่เกิน)
- แผงสมาชิก: ปุ่ม "+ เพิ่มเพื่อน" → `POST /api/friend-requests` (409 = แจ้งว่าซ้ำอย่างสุภาพ)
- **วิดีโอคอล:**
  - GROUP → เข้าคอลอัตโนมัติหลัง WS พร้อม (getUserMedia จะขอสิทธิ์ทันที)
  - DIRECT → **ไม่ auto**; ปุ่ม 📹 ขวาบน → INVITE flow (หัวข้อ 3); `?join=1` = auto เริ่มคอล (หน้าปลายทางหลังกดรับสาย)

### 📄 `/friends` (สไตล์ Messenger)
- ซ้าย: "แชทล่าสุด" จาก `GET /api/chats` (กรอง `roomType === "DIRECT"`, เรียงอยู่แล้ว, badge unread) + เพื่อน + คำขอเพื่อน (✓/✕ → accept/decline) + discover (`sendRequest`) + outgoing
- ขวา: หน้าต่างคุยในหน้า — `POST /api/chats/direct/{friendId}` เปิด/สร้างห้อง → subscribe topic + โหลดประวัติ + ส่งข้อความ (text ผ่าน WS, รูปผ่าน REST) + ลบข้อความตัวเอง
- 📹 ขวาบนของหน้าต่างคุย → INVITE flow; 💔 → `DELETE /api/friends/{friendId}`
- กดรับสายจากหน้านี้ → navigate ไป `/chat?roomId=X&join=1`

### 📄 `/profile`
- `GET /api/users/me` เติมฟอร์ม; `POST /api/users/me` บันทึก (รูป = base64 dataURL)
- interests: `GET /api/interests` + `GET /api/users/me/interests` → chips เลือก; `PUT /api/users/me/interests` บันทึก

---

## 6. สรุปข้อห้าม/ข้อควรระวัง (เจอจริงมาแล้วทั้งหมด)

1. **`GET /api/friends` ใช้ `friendId`** — ไม่ใช่ `id` (ผิดแล้วจะได้ `/undefined` → 400)
2. **publish ต่อเมื่อ `onConnect` แล้วเท่านั้น** และถ้ามี client ค้าง connecting อยู่ **อย่าสร้าง client ใหม่ทับ** — คิว callback ไว้
3. **ส่งรูปผ่าน REST เท่านั้น** — base64 ใหญ่เกิน WS frame
4. **`pagehide` เท่านั้น** — ห้าม `unload`/`beforeunload` (Chrome บล็อก)
5. **native WebSocket เท่านั้น** — ไม่มี SockJS ฝั่ง server
6. ห้อง GROUP ไม่มีประวัติ/unread — อย่าออกแบบ UI ให้ย้อนดู
7. สนทนา OFFER ต้องตอบ ANSWER ทันทีด้วย `setLocalDescription()` (no-arg) — และต้องมี perfect negotiation กัน glare
8. สิทธิ์กล้องทำงานเฉพาะ **HTTPS หรือ localhost** — ทดสอบมือถือผ่าน HTTPS tunnel
9. ห้อง DIRECT ชื่อใน DB = `"DIRECT"` — UI ต้องโชว์ชื่อเพื่อนจาก members แทน
10. credential TURN (Metered) เก็บนอก git — อย่าฝังในไฟล์ที่ commit
