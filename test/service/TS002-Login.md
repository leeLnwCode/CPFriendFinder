# TS002 - Login Service

| Field | Detail |
|:--|:--|
| Project ID | CPFriendFinder |
| Scenario ID / Use Case | TS002 / UC-AUTH-02 |
| Module | AuthService.login() |
| Test Class | `AuthServiceLoginTest` |
| Test Level | Unit Test |
| Test Method | Equivalence Class (EC), source-based cases, Mockito |
| Test Result | **11/11 Passed** |
| Test Date | 2026-09-30 |

## Test Scenario

ตรวจสอบการเข้าสู่ระบบด้วยข้อมูลถูกต้อง/ไม่ถูกต้อง สถานะบัญชี และการปรับรูปแบบอีเมล

**Preconditions:** เตรียม LoginRequest และ stub ผลค้นหาผู้ใช้กับการตรวจรหัสผ่านตามแต่ละกรณี

**Mocks / Stubs:** Mock UserRepository และ PasswordEncoder; ไม่เชื่อมต่อฐานข้อมูลจริง

## Test Cases and Results

| Case ID | Test Condition | Expected Result | Actual Result | Status |
|:--:|:--|:--|:--|:--:|
| LOGIN-001 | เข้าสู่ระบบด้วยข้อมูลถูกต้อง | Returns the authenticated user | Returned active User; password verified | Pass |
| LOGIN-002 | ส่ง LoginRequest เป็น null | Throws "Login request is required" | Exception and dependency assertions passed | Pass |
| LOGIN-003 | ไม่ระบุอีเมล (null) | Throws "Email is required" | Exception and dependency assertions passed | Pass |
| LOGIN-004 | อีเมลเป็นช่องว่าง | Throws "Email is required" | Exception and dependency assertions passed | Pass |
| LOGIN-005 | ไม่ระบุรหัสผ่าน (null) | Throws "Password is required" | Exception and dependency assertions passed | Pass |
| LOGIN-006 | รหัสผ่านเป็นช่องว่าง | Throws "Password is required" | Exception and dependency assertions passed | Pass |
| LOGIN-007 | ไม่พบอีเมลในระบบ | Throws "Invalid email or password" | Exception and dependency assertions passed | Pass |
| LOGIN-008 | บัญชีถูก BLOCKED | Throws "User account is not active" | BLOCKED account rejected | Pass |
| LOGIN-009 | รหัสผ่านไม่ถูกต้อง | Throws "Invalid email or password" | Incorrect password rejected | Pass |
| LOGIN-010 | อีเมลมีตัวพิมพ์ใหญ่และช่องว่าง | Looks up the normalized email | Value and dependency assertions passed | Pass |
| LOGIN-011 | บัญชี BLOCKED; ไม่เรียกตรวจรหัสผ่าน | Rejects before checking password (no matches call) | Rejected before password check | Pass |

## Execution Summary

| Item | Recorded Result |
|:--|:--|
| Scenario cases | 11/11 Passed |
| Run date/time (ICT) | 2026-09-30 15:00:47 (UTC+07:00) |
| Full Maven suite at that stage | 24/24 Passed |
| Failed / Errors / Skipped | 0 / 0 / 0 |
| Result source | Local Maven / JUnit output supplied by tester |

## Scope and Limitations

Mock UserRepository และ PasswordEncoder; ไม่เชื่อมต่อฐานข้อมูลจริง ผลที่บันทึกเป็นผลตาม JUnit assertions ของชุดทดสอบนี้ ไม่ใช่ผลการทดสอบเบราว์เซอร์จริงหรือฐานข้อมูลจริง
