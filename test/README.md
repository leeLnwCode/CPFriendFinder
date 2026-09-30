# CPFriendFinder - Test Documentation

**Course:** CP353002 Principles of Software Design and Development  
**Tester:** Jiratchaya Paocanthuek  
**Latest execution:** 2026-09-30 18:13:25 (UTC+07:00)

เอกสารชุดนี้เก็บ Test Plan, Test Case, ผลทดสอบ และประเด็นที่พบจากการตรวจโค้ดของส่วน Authentication และ Web Flow ที่พัฒนาแล้วในปัจจุบัน อ้างอิงแนวทางเอกสาร SQA โดยแยก Expected Result, Actual Result, Status และขอบเขตของ Mock ออกจากกัน

## Test Status

| Test Suite | Passed / Total | Test Level |
|:--|--:|:--|
| Registration Service | 12/12 | Unit Test (Mockito) |
| Login Service | 11/11 | Unit Test (Mockito) |
| Authentication Controller | 10/10 | Standalone MVC |
| Authentication Security | 9/9 | MVC Slice / Security |
| Web Flow | 8/8 | Full Spring Web Context (mock service) |
| Application Context | 1/1 | Spring context startup |
| **Full Maven Suite** | **51/51** | **BUILD SUCCESS** |

50 รายการมี Test Case ID อยู่ใน Workbook (TS001–TS005) อีก 1 รายการเป็น `FriendApplicationTests.contextLoads()` รวมผล Maven 51 Tests ผ่านทั้งหมด ไม่มี Failed, Errors หรือ Skipped

## Documents

| File | Detail |
|:--|:--|
| `test-plan/test-plan.md` | ขอบเขต วิธีออกแบบ สภาพแวดล้อม และเกณฑ์ผ่าน |
| `test-cases/CPFriendFinder_TestCases.xlsx` | Test Scenario, Case Design/Results, RTM, Summary และ Execution Log |
| `service/TS001-Registration.md` | Registration Service test cases |
| `service/TS002-Login.md` | Login Service test cases |
| `api/TEST-CASES-AUTH-CONTROLLER.md` | TS003 Controller test cases |
| `api/TS004-AuthSecurity.md` | TS004 Security test cases |
| `api/TS005-WebFlow.md` | TS005 Web Flow test cases |
| `reports/test-summary.md` | ผลล่าสุดและประวัติการทดสอบ |
| `reports/web-flow-2026-09-30.md` | ผล Web Flow และ Full Suite ล่าสุด |
| `reports/review-notes.md` | Code review observations (ไม่ใช่ผล Fail) |
| `evidence/` | หลักฐานเพิ่มเติม; Surefire files อยู่ในเครื่องผู้ทดสอบ |

Historical reports ที่อยู่ใน Repository ให้เก็บไว้ตามผลการรันจริงในแต่ละช่วง ไม่ต้องเปลี่ยนผล 24/34/43 ให้กลายเป็น 51

## Execution

```bash
./mvnw -Djava.version=21 clean test
```

**Result:** 51/51 Passed; 0 Failures, 0 Errors, 0 Skipped; BUILD SUCCESS; 01:29 min  
**Evidence:** Local Maven output and `target/surefire-reports/`; ไม่ใช่ผล CI บน GitHub Actions

**Test Boundary:** ใช้ Mock ใน Service, Controller, Security และ Web Flow ตามที่แต่ละ Scenario ระบุ จึงยังไม่ได้ยืนยันการทำงานผ่านเบราว์เซอร์จริง การอ่าน/เขียน PostgreSQL จริง หรือการทำงานครบทุกฟีเจอร์ของระบบ
