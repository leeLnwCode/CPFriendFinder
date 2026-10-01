# CPFriendFinder - Test Plan

## 1. Objective
ตรวจสอบพฤติกรรมหลักของ Backend ใน branch `develop` โดยเริ่มงานทดสอบใหม่เป็นลำดับขั้น และบันทึก Test Case / Actual Result / Evidence ให้ตรวจสอบย้อนหลังได้ง่าย

## 2. Current Scope
โค้ด Backend ปัจจุบันมี Controller หลัก 3 ตัว (`AuthController`, `InterestController`, `PageController`) และ Service หลัก 2 ตัว (`AuthService`, `InterestService`) จึงวางแผนทดสอบตามพฤติกรรมของระบบ ไม่ทดสอบทุก Model class แยกไฟล์โดยอัตโนมัติ

### Planned Scenarios
- TS001 Registration Service
- TS002 Login Service
- TS003 Authentication Controller
- TS004 Spring Security
- TS005 Interest Service / API
- TS006 Web Flow / Static Resources — Planned, รอ Frontend บน `develop`

## 3. Test Levels
- Unit Test: Service logic โดย Mock dependency ภายนอก
- MVC Slice Test: Controller HTTP request/response
- Security MVC Test: SecurityFilterChain และ session behavior
- Web-context Integration / Smoke Test: ทำเมื่อ Frontend/Static resources อยู่บน `develop`
- Real PostgreSQL / Browser E2E: ยังไม่รวมในรอบเริ่มต้น

## 4. Priorities
1. Authentication Service
2. Authentication Controller
3. Security
4. Interest API
5. Web Flow หลัง Frontend พร้อม

## 5. Test Design Rules
- ทดสอบตาม requirement และ behavior ที่มีอยู่จริง
- Positive, Negative, Boundary, Validation และ Session/Security cases ตามความเหมาะสม
- ไม่เพิ่ม test เพียงเพื่อให้จำนวนเยอะ
- Model/DTO ที่มีเพียง field mapping หรือ getter/setter ไม่จำเป็นต้องมี unit test แยก เว้นแต่มี logic หรือ constraint สำคัญ
- S3/Storage จะถูก Mock ใน Service Unit Test; ไม่ใช้ credential จริง
- Expected Result ต้องมาจาก behavior/requirement ปัจจุบัน ไม่แก้ Expected เพื่อให้ test ผ่าน

## 6. Evidence
หลังรันแต่ละ scenario ให้บันทึก:
- Maven command
- จำนวน Tests / Passed / Failed / Errors / Skipped
- Run date/time
- Actual Result ใน Excel
- Report markdown เฉพาะเมื่อ scenario รันจริงแล้ว

## 7. Commit Plan
- Commit 01: SQA structure + Test Plan + Excel skeleton
- Commit 02: TS001 Registration tests + result update
- Commit 03: TS002 Login tests + result update
- Commit 04: TS003 Auth Controller tests + result update
- Commit 05: TS004 Security tests + result update
- Commit 06: TS005 Interest tests + result update
- Commit 07: TS006 Web Flow tests หลัง Frontend พร้อม
- Final commit: Summary / review notes / final workbook update
