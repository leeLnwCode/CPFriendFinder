# CPFriendFinder - Test Summary Report

| Field | Recorded Value |
|:--|:--|
| Tester | Jiratchaya Paocanthuek |
| Date | 2026-09-30 |
| Execution environment | Local Windows, JDK 21, Maven Wrapper |
| Command | `./mvnw -Djava.version=21 clean test` |
| Completed | 18:13:25 (UTC+07:00) |
| Duration | 01:29 min |
| Build result | **BUILD SUCCESS** |

## 1. Latest Test Results

| Test Class | Tests | Passed | Failed | Errors | Skipped |
|:--|--:|--:|--:|--:|--:|
| `FriendApplicationTests` | 1 | 1 | 0 | 0 | 0 |
| `AuthServiceRegisterTest` | 12 | 12 | 0 | 0 | 0 |
| `AuthServiceLoginTest` | 11 | 11 | 0 | 0 | 0 |
| `AuthControllerMvcTest` | 10 | 10 | 0 | 0 | 0 |
| `AuthSecurityWebMvcTest` | 9 | 9 | 0 | 0 | 0 |
| `WebFlowIntegrationTest` | 8 | 8 | 0 | 0 | 0 |
| **Total** | **51** | **51** | **0** | **0** | **0** |

Workbook มี 50 Test Cases (TS001–TS005) ผ่านทั้งหมด และมี Spring Application Context Test อีก 1 รายการ รวมผล Maven 51/51 ผ่าน

## 2. Execution History

| Completed (2026-09-30, ICT) | Test Scope | Result | Duration |
|:--|:--|--:|--:|
| 15:00:47 | Service + application context | 24/24 Pass | Not recorded |
| 16:23:47 | Service + Controller + application context | 34/34 Pass | 55.244 s |
| 17:09:30 | เพิ่ม Security MVC | 43/43 Pass | 41.638 s |
| 18:13:25 | เพิ่ม Web Flow | **51/51 Pass** | **01:29 min** |

## 3. Method and Test Boundary

Service Unit Tests จำลอง Repository/PasswordEncoder; Controller, Security MVC และ Web Flow จำลอง `AuthService` แม้ TS005 โหลด Spring Web Context และ Security filters จริง ผลดังกล่าวจึงไม่ใช่การยืนยัน Browser E2E, PostgreSQL integration หรือทุกฟีเจอร์ของระบบ

## 4. Evidence and Follow-up

ผลการรันมาจาก Local Maven output ที่ผู้ทดสอบบันทึกไว้ โดยมีรายงานแยกคลาสใน `target/surefire-reports/` (ไม่ได้ Commit ไฟล์จากโฟลเดอร์ `target/`) รายงานรอบล่าสุดอยู่ที่ `web-flow-2026-09-30.md`; ประเด็นตรวจโค้ดอยู่ที่ `review-notes.md` แยกจาก Defect ที่ยืนยันด้วยการทดสอบ
