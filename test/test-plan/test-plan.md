# CPFriendFinder - Test Plan

## 1. Objective
ทดสอบ Backend บน branch `develop` แบบเป็นลำดับ โดยเก็บ Test Design, Mock/Stub, Test Data, Actual Result และหลักฐานการรันให้ตรวจย้อนหลังได้

## 2. Scope
- TS001 Registration Service
- TS002 Login Service
- TS003 Authentication Controller
- TS004 Spring Security
- TS005 Interest Service / API
- TS006 Web Flow / Static Resources — รอ Frontend บน `develop`

ไม่สร้าง Test แยกให้ทุก Model/DTO หากไม่มี logic ที่ต้องตรวจเฉพาะ

## 3. Test Levels
- Unit Test: JUnit 5 + Mockito
- MVC Slice Test: MockMvc
- Security MVC Test: SecurityConfig จริงใน MVC slice
- Web-context Smoke Test: ทำเมื่อ Frontend พร้อม
- Browser E2E / Real PostgreSQL / Real S3: ยังไม่รวมในรอบเริ่มต้น

## 4. Test Data and Mock Strategy
- UserRepository / InterestRepository: จำลองผลค้นหา/บันทึก
- PasswordEncoder: จำลอง encode/matches
- StorageTool / S3Config: จำลองการ upload และ endpoint; ไม่ใช้ credential จริง
- AuthService / InterestService: mock ใน Controller slice
- Session: ใช้ MockHttpSession ใน MVC test

## 5. Evidence
หลังรันแต่ละ scenario บันทึก:
- Maven command
- วัน/เวลา
- Tests / Passed / Failed / Errors / Skipped
- Actual Result และ Status ใน Excel
- JUnit method
- Defect ID เมื่อยืนยัน defect ได้จริง

## 6. Commit Plan
- Commit 01: initial SQA structure (ทำแล้ว)
- Commit 02: refine workbook/test plan structure
- Commit 03: TS001 Registration tests + execution result
- Commit 04: TS002 Login tests + execution result
- Commit 05: TS003 Auth Controller tests + execution result
- Commit 06: TS004 Security tests + execution result
- Commit 07: TS005 Interest tests + execution result
- Commit 08: TS006 Web Flow หลัง Frontend พร้อม
- Final: summary / review notes / final workbook
