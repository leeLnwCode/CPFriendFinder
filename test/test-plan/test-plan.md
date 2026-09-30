# CPFriendFinder - Test Plan

| Field | Detail |
|:--|:--|
| Project | CPFriendFinder |
| Course | CP353002 Principles of Software Design and Development |
| Tester | Jiratchaya Paocanthuek |
| Prepared / Executed | 2026-09-30 |
| Latest verified run | 18:13:25 (UTC+07:00) |
| Environment | Local Windows, Git Bash, Eclipse Temurin JDK 21, Maven Wrapper |

## 1. Objective

ทดสอบความถูกต้องของการสมัครสมาชิกและเข้าสู่ระบบ รวมถึงการตอบสนองของหน้าเว็บและ Authentication API ภายใต้โค้ดที่มีอยู่ในรอบนี้ พร้อมเก็บ Test Case และผลการรันให้ตรวจสอบย้อนหลังได้

## 2. Test Scope

| Scenario | Component | Test Level / Method | Result |
|:--:|:--|:--|--:|
| TS001 | `AuthService.register()` | Unit / Mockito / EC + BVA | 12/12 Pass |
| TS002 | `AuthService.login()` | Unit / Mockito / EC + source-based | 11/11 Pass |
| TS003 | `AuthController` | Standalone MockMvc | 10/10 Pass |
| TS004 | Auth API + `SecurityConfig` | `@WebMvcTest` + MockMvc | 9/9 Pass |
| TS005 | Pages, resources, Auth API + Security | `@SpringBootTest` + `@AutoConfigureMockMvc` | 8/8 Pass |
| Context | Application startup | `@SpringBootTest` | 1/1 Pass |
| **Total** | **Current test scope** | **Local Maven full suite** | **51/51 Pass** |

## 3. Test Method and Mock Dependencies

ใช้ Equivalence Class กับข้อมูลถูกต้อง/ไม่ครบ/ไม่ถูกต้อง และ Boundary Value Analysis กับรหัสผ่าน 7–8 ตัวอักษร ร่วมกับ Test Case ที่ตรวจการเปลี่ยนรูปแบบอีเมล สถานะบัญชี HTTP status, JSON response และ Session ตามพฤติกรรมของโค้ด

| Scenario | Real Components | Mock / Stub |
|:--:|:--|:--|
| TS001 | `AuthService.register()` | `UserRepository`, `PasswordEncoder` |
| TS002 | `AuthService.login()` | `UserRepository`, `PasswordEncoder` |
| TS003 | `AuthController` + Standalone MockMvc | `AuthService` |
| TS004 | Spring MVC slice, `SecurityConfig` | `AuthService` |
| TS005 | Spring application/web context, MVC, Security filters, page/resource mappings | `AuthService` |

TS005 เป็นการทดสอบ Web Context ไม่ใช่ End-to-End Test ผ่านเบราว์เซอร์ และไม่ได้ทดสอบการบันทึกข้อมูลกับ PostgreSQL จริง

## 4. Test Environment and Execution

| Item | Value |
|:--|:--|
| Runtime | JDK 21 |
| Build | Maven Wrapper |
| Framework | JUnit 5, Mockito, Spring MockMvc |
| Command | `./mvnw -Djava.version=21 clean test` |
| Evidence | Local Maven output, `target/surefire-reports/` |
| Latest run | 2026-09-30 18:13:25 (ICT); 01:29 min |

## 5. Pass Criteria and Results

กรณีทดสอบจะเป็น Pass เมื่อ Assertions ตรงกับ Expected Result และไม่มี Failure หรือ Error ในรอบนั้น บันทึกผลแยกรายการใน Workbook และเอกสาร TS001–TS005

| Item | Result |
|:--|--:|
| Designed cases | 50 |
| Executed cases | 50 |
| Passed cases | 50 |
| Failed / Blocked / Not Run | 0 / 0 / 0 |
| Additional context test | 1 Pass |
| **Full Maven Suite** | **51/51 Pass** |

## 6. Exclusions and Follow-up

ผลนี้ครอบคลุมเฉพาะ Authentication และ Web Flow ที่มีอยู่ในรอบทดสอบ ไม่รวมการกด Register/Login ผ่าน JavaScript บนเบราว์เซอร์จริง การเข้าหน้า `/home` หลัง API Login ด้วย Spring Security Authentication การเขียน/อ่านฐานข้อมูลจริง หรือฟีเจอร์ Chat, Friend Request, Matching และ Room ที่ยังพัฒนา/รวมโค้ดไม่ครบ ประเด็นจาก Code Review บันทึกแยกใน `test/reports/review-notes.md` และยังไม่ถือเป็นผล Fail ของ 51 Tests
