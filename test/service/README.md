# Service Test Scenarios

| Scenario | Target | Method | Mock Dependencies | Result | Document |
|:--:|:--|:--|:--|--:|:--|
| TS001 | `AuthService.register()` | EC / BVA / JUnit 5 | `UserRepository`, `PasswordEncoder` | 12/12 Pass | `TS001-Registration.md` |
| TS002 | `AuthService.login()` | EC / Source-based / JUnit 5 | `UserRepository`, `PasswordEncoder` | 11/11 Pass | `TS002-Login.md` |

ผลนี้เป็น Unit Test โดยจำลอง Dependency ไม่มีการอ่านหรือเขียนฐานข้อมูลจริง รายละเอียดแต่ละกรณีรวมถึง Expected/Actual อยู่ในไฟล์ Scenario และ Workbook
