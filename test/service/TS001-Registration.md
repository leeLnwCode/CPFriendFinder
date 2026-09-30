# TS001 - Registration Service

| Field | Detail |
|:--|:--|
| Project ID | CPFriendFinder |
| Scenario ID / Use Case | TS001 / UC-AUTH-01 |
| Module | AuthService.register() |
| Test Class | `AuthServiceRegisterTest` |
| Test Level | Unit Test |
| Test Method | Equivalence Class (EC), Boundary Value Analysis (BVA), Mockito |
| Test Result | **12/12 Passed** |
| Test Date | 2026-09-30 |

## Test Scenario

ตรวจสอบการสมัครสมาชิก ทั้งข้อมูลปกติ ข้อมูลไม่ครบ รหัสผ่านที่ขอบเขต และอีเมลซ้ำ

**Preconditions:** เตรียม RegisterRequest ตามเงื่อนไขของแต่ละกรณี และ stub เฉพาะ dependency ที่ถูกเรียกจริง

**Mocks / Stubs:** Mock UserRepository และ PasswordEncoder; ไม่เชื่อมต่อฐานข้อมูลจริง

## Test Cases and Results

| Case ID | Test Condition | Expected Result | Actual Result | Status |
|:--:|:--|:--|:--|:--:|
| REG-001 | สมัครสมาชิกด้วยข้อมูลครบถ้วน | Returns a new user; save is called once | Returned User; save/lookup assertions passed | Pass |
| REG-002 | ส่ง RegisterRequest เป็น null | Throws "Registration request is required" | Exception and dependency assertions passed | Pass |
| REG-003 | ไม่ระบุอีเมล (null) | Throws "Email is required" | Exception and dependency assertions passed | Pass |
| REG-004 | ระบุอีเมลเป็นช่องว่าง | Throws "Email is required" | Exception and dependency assertions passed | Pass |
| REG-005 | ไม่ระบุรหัสผ่าน (null) | Throws "Password is required" | Exception and dependency assertions passed | Pass |
| REG-006 | ระบุรหัสผ่านเป็นช่องว่าง | Throws "Password is required" | Exception and dependency assertions passed | Pass |
| REG-007 | รหัสผ่านสั้นกว่าเกณฑ์: 7 ตัวอักษร | Throws "Password must be at least 8 characters" | Rejected 7-character password as asserted | Pass |
| REG-008 | รหัสผ่านเท่าขอบเขต: 8 ตัวอักษร | Password is accepted and user is saved | Accepted 8-character password as asserted | Pass |
| REG-009 | สมัครสมาชิกด้วยอีเมลที่มีอยู่แล้ว | Throws "Email already exists"; user is not saved | Duplicate rejected; save not called | Pass |
| REG-010 | อีเมลมีตัวพิมพ์ใหญ่และช่องว่าง | Trims and lowercases email before saving | Value and dependency assertions passed | Pass |
| REG-011 | ตรวจการเข้ารหัสรหัสผ่านและสถานะ ACTIVE | Stores encoded password and sets status to ACTIVE | Value and dependency assertions passed | Pass |
| REG-012 | อีเมลว่าง; dependency ต้องไม่ถูกเรียก | Rejects input without calling repository or encoder | Exception and dependency assertions passed | Pass |

## Execution Summary

| Item | Recorded Result |
|:--|:--|
| Scenario cases | 12/12 Passed |
| Run date/time (ICT) | 2026-09-30 15:00:47 (UTC+07:00) |
| Full Maven suite at that stage | 24/24 Passed |
| Failed / Errors / Skipped | 0 / 0 / 0 |
| Result source | Local Maven / JUnit output supplied by tester |

## Scope and Limitations

Mock UserRepository และ PasswordEncoder; ไม่เชื่อมต่อฐานข้อมูลจริง ผลที่บันทึกเป็นผลตาม JUnit assertions ของชุดทดสอบนี้ ไม่ใช่ผลการทดสอบเบราว์เซอร์จริงหรือฐานข้อมูลจริง
