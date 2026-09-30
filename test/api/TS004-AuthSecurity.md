# TS004 - Authentication with Spring Security

| Field | Detail |
|:--|:--|
| Project ID | CPFriendFinder |
| Scenario ID / Use Case | TS004 / UC-AUTH-04 |
| Module | AuthController / SecurityConfig |
| Test Class | `AuthSecurityWebMvcTest` |
| Test Level | MVC Slice Test |
| Test Method | @WebMvcTest(AuthController.class) + @Import(SecurityConfig.class) + MockMvc |
| Test Result | **9/9 Passed** |
| Test Date | 2026-09-30 |

## Test Scenario

ตรวจสอบ Authentication API ผ่าน Spring MVC และ SecurityFilterChain รวมถึง CSRF configuration ตามระบบปัจจุบัน

**Preconditions:** ใช้ MVC test slice ที่นำเข้า SecurityConfig และเตรียม HttpSession เมื่อทดสอบ flow ที่เกี่ยวข้อง

**Mocks / Stubs:** Mock AuthService; ใช้ SecurityConfig จริงใน MVC slice โดยไม่เชื่อมต่อฐานข้อมูล

## Test Cases and Results

| Case ID | Test Condition | Expected Result | Actual Result | Status |
|:--:|:--|:--|:--|:--:|
| SEC-001 | สมัครสมาชิกแบบ anonymous โดยไม่มี CSRF token | 201 Created; response omits password fields | HTTP 201; no password fields | Pass |
| SEC-002 | Login แบบ anonymous โดยไม่มี CSRF token | 200 OK; userId stored in session | HTTP 200; userId stored in Session | Pass |
| SEC-003 | ตรวจ Session แบบ anonymous | 200 OK; userId is null | HTTP 200; null userId | Pass |
| SEC-004 | Login แล้วตรวจ Session เดียวกัน | 200 OK; the same userId is returned | HTTP 200; same userId | Pass |
| SEC-005 | Logout โดยไม่มี CSRF token | 204 No Content; session invalidated | HTTP 204; Session invalidated | Pass |
| SEC-006 | ส่ง JSON สมัครสมาชิกผิดรูปแบบ | 400 Bad Request | HTTP 400 | Pass |
| SEC-007 | Login โดยไม่ส่ง body | 400 Bad Request | HTTP 400 | Pass |
| SEC-008 | Logout แบบ anonymous | 204 No Content | HTTP 204 | Pass |
| SEC-009 | ส่ง JSON Login ผิดรูปแบบ | 400 Bad Request | HTTP 400 | Pass |

## Execution Summary

| Item | Recorded Result |
|:--|:--|
| Scenario cases | 9/9 Passed |
| Run date/time (ICT) | 2026-09-30 17:09:30 (UTC+07:00) |
| Full Maven suite at that stage | 43/43 Passed |
| Failed / Errors / Skipped | 0 / 0 / 0 |
| Result source | Local Maven / JUnit output supplied by tester |

## Scope and Limitations

Mock AuthService; ใช้ SecurityConfig จริงใน MVC slice โดยไม่เชื่อมต่อฐานข้อมูล ผลที่บันทึกเป็นผลตาม JUnit assertions ของชุดทดสอบนี้ ไม่ใช่ผลการทดสอบเบราว์เซอร์จริงหรือฐานข้อมูลจริง

การยกเว้น CSRF ของ `/api/**` เป็นการตั้งค่าปัจจุบันที่ทดสอบ ไม่ใช่การรับรองว่าการตั้งค่านี้เหมาะสำหรับ Production
