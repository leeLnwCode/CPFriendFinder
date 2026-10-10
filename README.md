# CP Friend Finder

**CP Friend Finder** คือระบบช่วยนักศึกษาภายในคณะค้นหาและทำความรู้จักเพื่อนใหม่ที่มีความสนใจ
หรือไลฟ์สไตล์ใกล้เคียงกัน ออกแบบมาเพื่อ "ชาว Introvert" โดยเฉพาะ
ระบบจับคู่เพื่อนจากความสนใจร่วม (Interest Matching), มีห้องแชทกลุ่ม/แชทส่วนตัว,
แชทสุ่มกับคนไม่รู้จัก, คำขอเป็นเพื่อน, การแจ้งเตือน และสายเสียง (Voice Call) ภายในแอปเดียว
พัฒนาด้วย Spring Boot ตามสถาปัตยกรรมแบบ Layered Architecture

---

## สมาชิกกลุ่ม

| ลำดับ | ชื่อ-นามสกุล | รหัสนักศึกษา | Section | Branch | หน้าที่รับผิดชอบ |
|---|---|---|---|---|---|
| 1 | ปองภพ ศรีรักษ์ | 673380279-7 | 01 | `phongphop_673380279-7_01` | Backend Core (User/Profile), สถาปัตยกรรม Layered, SOLID analysis, README |
| 2 | ปิยพนธ์ แก้วเก็บคำ | 673380050-9 | 01 | `piyapon_673380050-9_01` | Database/Entity/Repository ทั้งระบบ (14 ตาราง), CRUD ห้องแชทและเพื่อน, schema |
| 3 | โยโกะ คามิโจ | 673380302-8 | 01 | `Yoko_673380302-8_01` | Frontend Thymeleaf ทุกหน้า, WebSocket UI, หน้าสุ่มคุย/Voice call |
| 4 | จิรัชญา เป้าจันทึก | 673380510-1 | 01 | `jiratchaya_673380510-1_01` | DTO/Validation, REST API, Testing (JUnit 5/Mockito), Test Report |
| 5 | อภิวิชญ์ เอกะ | 673380517-7 | 01 | `apiwich_673380517-7_01` | Matching (Strategy), Notification (Observer), Docker/CI, WebRTC/TURN, Deployment |



---

## Tech Stack

| หมวด | เทคโนโลยี |
|---|---|
| Backend | Spring Boot (Maven, Java 21) |
| Frontend | Thymeleaf + HTML/CSS/JavaScript |
| Database | PostgreSQL 16 (ใช้ H2 in-memory สำหรับ test) |
| ORM | Spring Data JPA (Hibernate) |
| Realtime | WebSocket (STOMP) + WebRTC Voice Call (Cloudflare TURN) |
| Security | Spring Security (session-based) + BCrypt password hashing |
| API Docs | springdoc-openapi (Swagger UI) |
| File Storage | AWS S3-compatible storage (รูปโปรไฟล์ / ไฟล์แนบ) |
| Testing | JUnit 5, Mockito, `@WebMvcTest`, Spring Boot Test |
| DevOps | Docker / docker-compose, GitHub Actions CI, Node.js gateway (`server.js`) |

---

## System Architecture

โปรเจกต์ใช้สถาปัตยกรรมแบบ **Layered Architecture** — Controller ไม่เรียก Repository ตรง ๆ
ทุกคำขอไหลผ่าน Service Layer เสมอ:

```
Presentation Layer   (Controller / RestController / Thymeleaf View)
        ↓
Service Layer        (Business Logic, Transaction)
        ↓
Repository Layer     (Data Access — Spring Data JPA)
        ↓
Domain / Entity      (Entity, Enum)  +  DTO + Mapper  +  Config / Exception
```

- **REST API** สำหรับทุกฟีเจอร์ (auth, users, friends, friend-requests, chats,
  interests, matching, notifications, call signaling)
- **WebSocket (STOMP)** สำหรับแชทสด, presence และ call signaling — ตรวจสิทธิ์ทุก subscription
- **WebRTC + TURN** สำหรับสายเสียงข้ามเครือข่าย (ICE config ออกผ่าน gateway เฉพาะผู้ที่ login)
- **Session-based authentication** (BCrypt) + **Global Exception Handler**
  (`@RestControllerAdvice`) + Bean Validation (`@Valid`)

ดูโครงสร้างคลาสจริงได้ที่:
`doc/diagrams/class/class-layered.png` (Layered dependencies) และ
`doc/diagrams/class/class-behavioral-patterns.png` (Strategy / Template Method / Observer)

---

## Database Design (ER Diagram)

ฐานข้อมูล PostgreSQL ประกอบด้วย **14 ตาราง** ครบทุกความสัมพันธ์:

- **One-to-One** — `users ↔ user_profiles`
- **One-to-Many** — `users → friend_requests`, `users → notifications`,
  `users → user_gallery_photos`, `chat_rooms → messages`, `chat_rooms → room_members`,
  `random_chat_sessions → random_chat_messages`
- **Many-to-Many (โบนัส)** — `users ↔ interests` ผ่าน `user_interests`,
  `chat_rooms ↔ interests` ผ่าน `room_interests`
- มี **Foreign Key Constraint, Unique Constraint, Check Constraint และ Index**
  ครบ พร้อมกำหนด Cascade / Fetch Type อย่างมีเหตุผล

เอกสารประกอบ:
- ER Diagram: `doc/diagrams/er/er-database.png` (+ `.svg`)
- Data Dictionary: `doc/data-dictionary.md` (ครบ 14 ตาราง ทุกคอลัมน์)
- Migration: `src/main/resources/schema.sql` + `data.sql`
  (บน PostgreSQL จริงใช้ Hibernate `ddl-auto=update` และ seed ความสนใจเริ่มต้นอัตโนมัติ)

---

## Installation & Setup

**สิ่งที่ต้องมี:** JDK 21, PostgreSQL 16 (หรือใช้ Docker), Git
(และ Node.js เฉพาะเมื่อใช้ voice call ผ่าน gateway)

```bash
git clone https://github.com/leeLnwCode/CPFriendFinder.git
cd CPFriendFinder
```

สร้างฐานข้อมูล `cpfriendfinder` แล้วตั้งค่า environment variables (ดูตัวอย่างใน `.env.example`):

| ตัวแปร | ค่าตัวอย่าง |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cpfriendfinder` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `<รหัสผ่านของคุณ>` |
| `AWS_ENDPOINT_URL_S3` / `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION` | ค่า S3-compatible storage (MinIO / R2) |

> ห้าม commit รหัสผ่านจริง — ใช้ environment variables เท่านั้น

---

## How to Run

```bash
# 1) รันด้วย Maven (แนะนำ)
DB_URL="jdbc:postgresql://localhost:5432/cpfriendfinder" \
DB_USERNAME=postgres DB_PASSWORD=<รหัสผ่าน> \
./mvnw spring-boot:run

# 2) หรือรันด้วย Docker
docker compose up --build
```

จากนั้นเปิดเบราว์เซอร์ที่ **http://localhost:8080** → ระบบพาไปหน้า Login
สมัครบัญชีใหม่ (รหัสผ่านอย่างน้อย 8 ตัวอักษร) แล้วเริ่มใช้งานได้ทันที

ฟีเจอร์หลัก: หน้าหลัก (ห้องแชท) · ส่องคน/สุ่มเพื่อน · เพื่อนของฉัน (แชท+คอล) ·
การแจ้งเตือน (คำขอเพื่อน/ข้อความ) · ตั้งค่า (โปรไฟล์/ธีม)

---

## API Documentation

**Swagger UI:** http://localhost:8080/swagger-ui.html

กลุ่ม endpoint หลัก (REST, session-based):

| กลุ่ม | Endpoint | คำอธิบาย |
|---|---|---|
| Auth | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` | สมัคร/ล็อกอิน/ออกจากระบบ |
| Users | `GET /api/users/me`, `PUT /api/users/me` | ดู/แก้ไขโปรไฟล์ |
| Interests | `GET /api/interests` | ความสนใจ |
| Friends | `GET /api/friends`, `DELETE /api/friends/{id}` | รายชื่อเพื่อน |
| Friend Requests | `POST /api/friend-requests`, `GET /api/friend-requests/incoming`, `POST .../accept`, `POST .../decline` | คำขอเป็นเพื่อน |
| Chats | `GET /api/chats`, `GET /api/chats/discover`, `POST /api/chats`, `POST /api/chats/{id}/join`, `GET /api/chats/{id}/messages`, `POST /api/chats/direct/{friendId}` | ห้องแชทและข้อความ |
| Matching | `GET /api/matching/recommendations/{userId}` | แนะนำเพื่อนตามความสนใจ |
| Notifications | `GET /api/notifications` | การแจ้งเตือน |
| WebSocket | `/ws` (STOMP) | แชทสด / presence / call signaling |

HTTP Status Codes ที่ใช้: `200 / 201 / 204 / 400 / 401 / 403 / 404 / 409 / 500`
พร้อม Error Response Format มาตรฐานจาก `GlobalExceptionHandler`

---

## How to Run Tests

```bash
./mvnw test
```

- ชุดทดสอบปัจจุบัน **259 tests** (JUnit 5 + Mockito + `@WebMvcTest` + H2)
  ครอบคลุม service, controller, security, WebSocket และ web-startup
- รายงานผลอยู่ที่ `target/surefire-reports/`
- แผนทดสอบและชุดทดสอบ E2E เพิ่มเติมอยู่ที่ `test/` (Robot Framework)

---

## Deployment URL

```
(https://life-train-operator-emily.trycloudflare.com/login)
```

แนวทางที่รองรับ: Render / Railway / Fly.io / VPS + Docker
พร้อม Cloud DB (Neon / Supabase / Railway Postgres) และ S3-compatible storage สำหรับรูปภาพ
(ดู `docs/public-setup.md` และ `docs/cloudflare-turn-setup.md` สำหรับ voice call)

---

## Project Structure

```
CPFriendFinder/
├── src/main/java/com/cp/friend/
│   ├── config/          # Security, WebSocket, S3, Seeder, Filter
│   ├── controller/      # RestController + PageController (Thymeleaf)
│   ├── dto/             # request/ + response/ (+ matching)
│   ├── event/           # FriendRequest events (Observer)
│   ├── exception/       # GlobalExceptionHandler, ApiResponse
│   ├── factory/         # NotificationFactory
│   ├── mapper/          # UserMapper, InterestMapper
│   ├── model/           # Entity ทั้งหมด (14 ตาราง)
│   ├── observer/        # NotificationObserver
│   ├── port/            # StoragePort, MessageNotifications (DIP)
│   ├── repository/      # Spring Data JPA repositories
│   ├── service/         # Business Logic (+ strategy/, impl/)
│   ├── strategy/        # MatchingStrategy, InterestMatchingStrategy
│   └── tools/           # StorageTool (S3 adapter)
├── src/main/resources/  # templates/, static/, schema.sql, data.sql
├── src/test/java/       # JUnit 5 + Mockito (259 tests)
├── doc/                 # data-dictionary, design-patterns,
│                        # solid-analysis, diagrams/ (ER, class, use-case, domain-model)
├── docs/                # คู่มือ deploy / TURN / screen sharing
├── test/                # แผนทดสอบ, test cases, Robot Framework E2E
├── docker-compose.yml   # PostgreSQL + App
├── server.js            # Node gateway (proxy + TURN ICE config)
└── .github/workflows/   # GitHub Actions CI (Build → Test)
```

---

## Git Workflow

```
main        (Production — merge ผ่าน Pull Request เท่านั้น)
  ↑
develop     (Integration — รวมงานทุกคน)
  ↑
ชื่อ_รหัสนักศึกษา_section   (Branch ส่วนตัวของแต่ละคน)
```

- ทุกคน commit/push ด้วยบัญชี GitHub ของตนเอง (≥ 15 meaningful commits)
- การรวมงานผ่าน Pull Request + Reviewer อย่างน้อย 1 คนในทีม
- Commit message ใช้รูปแบบ `<type>: <สิ่งที่ทำ>` เช่น `feat:`, `fix:`, `docs:`, `test:`

---

## Design Patterns

| Pattern | ปัญหาที่แก้ | ไฟล์/คลาสที่ใช้ |
|---|---|---|
| Strategy | เนื้อหาข้อความหลายประเภท (TEXT/IMAGE/FILE) และการจับคู่ | `MessageContentStrategy` + Text/Image/File, `MatchingStrategy` → `InterestMatchingStrategy` |
| Template Method | ขั้นตอนประมวลผลเนื้อหาตายตัว (normalize → validate → transform) | `AbstractMessageContentStrategy.process()` (final) |
| Observer | แจ้งเตือนเมื่อเกิดเหตุการณ์ โดยไม่ผูก service เข้าหากัน | `FriendRequestSent/Accepted/DeclinedEvent` → `NotificationObserver`, `NotificationFactory` |
| Repository | แยก data access ออกจาก business logic | Spring Data JPA repositories ทั้งหมด |
| DTO + Mapper | แยก Entity ออกจาก API contract | `dto/request`, `dto/response`, `mapper/` |

รายละเอียดเหตุผลฉบับเต็ม: `doc/design-patterns.md` และ `doc/solid-analysis.md`
