# CPFriendFinder — Tester Workspace

พื้นที่นี้เก็บแผนการทดสอบ Test Scenario/Test Case และรายงานตามแบบฝึก Lab #2 ที่ปรับเข้ากับ POS; โค้ด JUnit จริงอยู่ใน `src/test/java/` ตามมาตรฐาน Maven/Spring Boot ของ repo ปัจจุบัน

```text
test/
├── test-plan/test-plan.md                 แผน/ขอบเขต/สภาพแวดล้อม
├── test-cases/CPFriendFinder_TestCases.xlsx  Test Scenario + Case + Traceability + Summary
├── reports/baseline-2026-09-30.md        ผลรันเดิมที่ยืนยันแล้ว
├── reports/test-summary.md               สรุปผลตามหลักฐานที่มี
├── reports/review-notes.md               ประเด็นรอคุยกับ Backend/ทีม
├── evidence/README.md                    วิธีแนบ log และหลักฐาน
└── api/README.md                         ที่เก็บ API Test เมื่อมีผลรันจริง
```

## คำสั่งบน Git Bash
```bash
# Unit Test Register (ไม่ต่อฐานข้อมูลจริง)
./mvnw -Djava.version=21 -Dtest=AuthServiceRegisterTest test

# Unit Test Login
./mvnw -Djava.version=21 -Dtest=AuthServiceLoginTest test

# ทั้งสองคลาส (ใช้ -Dtest ใส่ class names คั่นด้วย comma)
./mvnw -Djava.version=21 -Dtest=AuthServiceRegisterTest,AuthServiceLoginTest test

# Full suite: contextLoads อาจต้องพึ่ง Spring configuration/ฐานข้อมูล
./mvnw -Djava.version=21 clean test
```

**ห้ามกรอก Pass ให้ Test ใหม่ก่อนรันจริง** ขณะนี้ baseline ที่รายงานได้คือ `FriendApplicationTests.contextLoads()` 1/1 ผ่านเท่านั้น

**สำหรับ repo กลุ่ม:** ใบงานบังคับโฟลเดอร์ `code/`, `test/`, `doc/`, `img/` แต่ซอร์สปัจจุบันยังอยู่ที่ root `src/` — **อย่าย้าย source** จนทีมตกลง migration ร่วมกัน เพื่อไม่ให้ frontend/backend/deploy แตกพร้อมกัน
