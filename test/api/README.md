# API and Web Test Scenarios

เก็บ Test Scenario ที่ทดสอบ Authentication Controller, Security และ Web Flow โดยใช้หัวข้อมาตรฐานเดียวกับ TS001–TS002: Test Scenario, Preconditions, Mocks / Stubs, Test Cases and Results, Execution Summary และ Scope and Limitations

| Scenario | Target | Mock / Stub | Result | Document |
|:--:|:--|:--|--:|:--|
| TS003 | Authentication Controller (Standalone MockMvc) | `AuthService` | 10/10 Pass | `TEST-CASES-AUTH-CONTROLLER.md` |
| TS004 | Authentication API + Security MVC | `AuthService` | 9/9 Pass | `TS004-AuthSecurity.md` |
| TS005 | Full Spring Web Context + resources | `AuthService` | 8/8 Pass | `TS005-WebFlow.md` |

**Covered endpoints:** `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth`, `POST /api/auth/logout` รวมทั้งหน้า `/login`, `/register`, `/home` และ Static Resources ที่ใช้ใน TS005

**Full-suite result:** 2026-09-30 18:13:25 ICT — 51/51 Pass; BUILD SUCCESS  
**Evidence:** Local Maven output / Surefire reports  
**Limit:** ไม่ได้ใช้ Browser E2E หรือฐานข้อมูลจริง และยังไม่รับรองว่าสำเร็จหลัง Login แล้วเข้า `/home` ได้
