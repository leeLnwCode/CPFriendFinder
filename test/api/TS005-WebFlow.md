# TS005 - Web Flow and Static Resource Smoke Test

| Field | Detail |
|:--|:--|
| Project ID | CPFriendFinder |
| Scenario ID / Use Case | TS005 / UC-WEB-01 |
| Module | PageController / AuthController / SecurityConfig / static resources |
| Test Class | `WebFlowIntegrationTest` |
| Test Level | Web-context Integration / Smoke Test |
| Test Method | @SpringBootTest + @AutoConfigureMockMvc |
| Test Result | **8/8 Passed** |
| Test Date | 2026-09-30 |

## Test Scenario

ตรวจสอบการเปิดหน้าเว็บ การให้บริการ Static Resources และ Authentication API ผ่าน Spring Web Context

**Preconditions:** ใช้ Full Spring Context; จัดเตรียมการตอบกลับ AuthService สำหรับการสมัครสมาชิก/เข้าสู่ระบบ

**Mocks / Stubs:** ใช้ Spring Context และ SecurityFilterChain จริง แต่ Mock AuthService; ไม่ทดสอบ PostgreSQL/JavaScript บนเบราว์เซอร์

## Test Cases and Results

| Case ID | Test Condition | Expected Result | Actual Result | Status |
|:--:|:--|:--|:--|:--:|
| WEB-001 | เปิดหน้า /login แบบ anonymous | 200 OK; renders login view | HTTP 200; login view | Pass |
| WEB-002 | เปิดหน้า /register แบบ anonymous | 200 OK; renders register view | HTTP 200; register view | Pass |
| WEB-003 | เข้า /home แบบ anonymous | 4xx; anonymous access to protected home denied | 4xx; anonymous access denied | Pass |
| WEB-004 | โหลดไฟล์ /css/register.css | 200 OK; stylesheet served | HTTP 200; CSS served | Pass |
| WEB-005 | โหลดไฟล์ /js/register.js | 200 OK; JavaScript served | HTTP 200; JavaScript served | Pass |
| WEB-006 | โหลดรูป /images/man.jpg | 200 OK; image served | HTTP 200; image served | Pass |
| WEB-007 | สมัครสมาชิกผ่าน Authentication API | 201 Created; public user fields; password omitted | HTTP 201; password fields excluded | Pass |
| WEB-008 | Login → ตรวจ Session → Logout → ตรวจ Session | 200 / 200 / 204 / 200; userId saved then cleared | HTTP 200/200/204/200; Session stored and invalidated | Pass |

## Execution Summary

| Item | Recorded Result |
|:--|:--|
| Scenario cases | 8/8 Passed |
| Run date/time (ICT) | 2026-09-30 18:13:25 (UTC+07:00) |
| Full Maven suite at that stage | 51/51 Passed |
| Failed / Errors / Skipped | 0 / 0 / 0 |
| Result source | Local Maven / JUnit output supplied by tester |

## Scope and Limitations

ใช้ Spring Context และ SecurityFilterChain จริง แต่ Mock AuthService; ไม่ทดสอบ PostgreSQL/JavaScript บนเบราว์เซอร์ ผลที่บันทึกเป็นผลตาม JUnit assertions ของชุดทดสอบนี้ ไม่ใช่ผลการทดสอบเบราว์เซอร์จริงหรือฐานข้อมูลจริง

WEB-003 ตรวจเพียงผู้ใช้ที่ยังไม่ได้ Login; ยังไม่ได้ยืนยันว่า Login ผ่าน API แล้วสามารถเข้า `/home` ได้ด้วย Spring Security Authentication
