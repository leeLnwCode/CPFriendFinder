# TS003 - Authentication Controller MVC

| Field | Detail |
|:--|:--|
| Project ID | CPFriendFinder |
| Scenario ID / Use Case | TS003 / UC-AUTH-03 |
| Module | AuthController |
| Test Class | `AuthControllerMvcTest` |
| Test Level | MVC Unit Test |
| Test Method | Standalone MockMvc + Mockito |
| Test Result | **10/10 Passed** |
| Test Date | 2026-09-30 |

## Test Scenario

ตรวจสอบ HTTP status, JSON response และการจัดการ HttpSession ใน Controller

**Preconditions:** สร้าง Standalone MockMvc ด้วย AuthController และกำหนดผลของ AuthService เฉพาะกรณีที่ต้องใช้

**Mocks / Stubs:** Mock AuthService; ไม่เริ่ม Spring Boot Context และไม่มี SecurityFilterChain

## Test Cases and Results

| Case ID | Test Condition | Expected Result | Actual Result | Status |
|:--:|:--|:--|:--|:--:|
| CTRL-001 | สมัครสมาชิกด้วย JSON ถูกต้อง | 201 Created; public user fields only; no password | HTTP 201; public user fields; no password fields | Pass |
| CTRL-002 | สมัครสมาชิกโดยไม่ส่ง body | 400 Bad Request; service not called | HTTP 400; AuthService not called | Pass |
| CTRL-003 | สมัครสมาชิกด้วย JSON ไม่ถูกต้อง | 400 Bad Request; service not called | HTTP 400; AuthService not called | Pass |
| CTRL-004 | Login สำเร็จและเก็บ userId ใน Session | 200 OK; userId saved in session | HTTP 200; userId stored in Session | Pass |
| CTRL-005 | Login โดยไม่ส่ง body | 400 Bad Request; service not called | HTTP 400; AuthService not called | Pass |
| CTRL-006 | Login ด้วย JSON ไม่ถูกต้อง | 400 Bad Request; service not called | HTTP 400; AuthService not called | Pass |
| CTRL-007 | ตรวจ Session หลัง Login | 200 OK; matching userId returned | HTTP 200; matching userId | Pass |
| CTRL-008 | ตรวจ Session โดยยังไม่ Login | 200 OK; userId is null (current controller behavior) | HTTP 200; userId is null | Pass |
| CTRL-009 | Logout เมื่อ Session มี userId | 204 No Content; session invalidated | HTTP 204; Session invalidated | Pass |
| CTRL-010 | Logout โดยยังไม่ Login | 204 No Content | HTTP 204 | Pass |

## Execution Summary

| Item | Recorded Result |
|:--|:--|
| Scenario cases | 10/10 Passed |
| Run date/time (ICT) | 2026-09-30 16:23:47 (UTC+07:00) |
| Full Maven suite at that stage | 34/34 Passed |
| Failed / Errors / Skipped | 0 / 0 / 0 |
| Result source | Local Maven / JUnit output supplied by tester |

## Scope and Limitations

Mock AuthService; ไม่เริ่ม Spring Boot Context และไม่มี SecurityFilterChain ผลที่บันทึกเป็นผลตาม JUnit assertions ของชุดทดสอบนี้ ไม่ใช่ผลการทดสอบเบราว์เซอร์จริงหรือฐานข้อมูลจริง
