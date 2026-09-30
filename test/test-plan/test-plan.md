# แผนการทดสอบ CPFriendFinder — ฉบับเริ่มต้น (v0.1)

**รายวิชา:** CP353002 Principles of Software Design and Development  
**ผู้รับผิดชอบการทดสอบ:** จิรัชญา เป้าจันทึก (Tester)  
**วันที่เริ่มต้น:** 30 กันยายน 2569  
**สถานะ:** เอกสารเริ่มต้น — ต้องปรับหลังรวมงานเข้ากับ `develop`  
**ฐานซอร์สชั่วคราว:** `piyapon_673380050-9_section1_(backend)` (ยังไม่ใช่ระบบรวม)

## 1. วัตถุประสงค์
ตรวจสอบความถูกต้องของฟังก์ชันและการทำงานร่วมกันของ CPFriendFinder พร้อมจัดเก็บหลักฐานที่ตรวจสอบย้อนกลับได้ โดยเริ่มจากฟังก์ชัน Authentication ที่ปรากฏในซอร์ส Backend แล้ว จากนั้นขยายไปยังฟีเจอร์อื่นเมื่อทีมยืนยันข้อกำหนดและ Merge เข้า `develop`

## 2. ขอบเขตปัจจุบันและขอบเขตถัดไป
| ขอบเขต | สถานะ | หลักฐาน/เงื่อนไข |
|---|---|---|
| Register / Login (`AuthService`) | พร้อมออกแบบและเขียน Unit Test | พบเมธอดใน branch Backend ที่ตรวจ |
| Auth API (`/api/auth/register`, `/login`, `GET /api/auth`, `/logout`) | วางแผน Controller/API Test | พบ Endpoint ใน `AuthController`; ยังไม่ได้รันแยก |
| Friend request / Room / Chat / Profile | รอยืนยันและระบบรวม | มีโมเดล/Repository บางส่วน แต่ยังไม่ถือว่า Feature ใช้งานได้ครบ |
| Integration / UAT / Regression | รอการ Merge เข้า `develop` | ทดสอบบนระบบที่รวม Frontend + Backend + Database จริง |
| Performance / Static Analysis | ทำเมื่อ Core Tests เสร็จ | เครื่องมือที่พิจารณา: JMeter / PMD / Checkstyle |

**นอกขอบเขตของระยะนี้:** การรับรองว่า REST API ทุก Endpoint, ระบบแชต หรือการ Deploy ทำงานครบถ้วน — ยังไม่มีผลรันทดสอบรองรับ

## 3. วิธีและระดับการทดสอบ
- **Unit Test:** JUnit 5 + Mockito ทดสอบ `AuthService` โดย Mock `UserRepository` และ `PasswordEncoder` เพื่อไม่ต้องเชื่อม PostgreSQL
- **การออกแบบ Test Case:** Equivalence Class สำหรับ null/blank/valid/invalid, Boundary Value สำหรับรหัสผ่านขั้นต่ำ 8 ตัว, และกรณีเงื่อนไขต่าง ๆ จากซอร์สจริง
- **Controller / API Test (ขั้นต่อไป):** ตรวจ HTTP Status, JSON Response, Session, Validation และ Error Response เมื่อทีมยืนยัน API contract
- **Integration / Regression (ขั้นต่อไป):** เรียกใช้งานระบบรวมหลัง PR เข้า `develop` ตรวจการเชื่อมต่อระหว่าง Layer และไม่ให้ฟังก์ชันเดิมเสีย
- **UAT (ขั้นต่อไป):** ตรวจ flow ตาม Use Case ของระบบที่ทีมตกลงร่วมกัน

## 4. สภาพแวดล้อมและข้อมูลทดสอบ
| รายการ | ค่าที่ตรวจ/กำหนด |
|---|---|
| ระบบปฏิบัติการ | Windows 11 (รายงานโดย Maven ในเครื่องผู้ทดสอบ) |
| Terminal | Git Bash |
| JDK / javac | Eclipse Temurin 21.0.11 |
| Maven Wrapper | Maven 3.9.16 |
| ซอร์สที่ใช้ในระยะเริ่มต้น | Branch Backend ของสมาชิก (ยังไม่ Merge) |
| คำสั่งชั่วคราว | `./mvnw -Djava.version=21 clean test` |
| Database | ไม่ใช้จริงในการทดสอบ `AuthService` แบบ Mock; ทดสอบ Integration ภายหลัง |

> **ข้อควรระวัง:** `pom.xml` ใน branch Backend ตั้ง `java.version=23` แต่เครื่อง Tester ใช้ Java 21 จึงต้อง Override ในคำสั่งชั่วคราว; ให้ทีมตกลงเวอร์ชัน Java ก่อนรวม `develop` ห้ามสรุปว่าเป็น defect ในโปรแกรมก่อนยืนยัน baseline ของทีม

## 5. เกณฑ์เริ่มและเกณฑ์ผ่าน
- **เริ่ม Unit Test:** ซอร์ส Backend ที่อ้างอิง Compile ได้, `UserRepository`/DTO/Service ที่ใช้ยังไม่เปลี่ยน API แบบทำให้ Test ใช้ไม่ได้
- **ผ่านราย Test Case:** Actual Result ตรง Expected Result และ JUnit assertion ผ่าน
- **ผ่านงานส่วน Tester:** มีการบันทึกผลรันจริง แสดงรายการที่ Pass/Fail/Blocked และหลักฐาน; ไม่รายงานว่า Test ที่ยังไม่ได้รันผ่านแล้ว
- **ผ่านระบบรวมก่อนส่ง:** ทีมตรวจสอบและให้ Test ที่กำหนดทั้งหมดผ่าน รวมถึง API/Integration/UAT ตาม Feature ที่ส่งจริง

## 6. ขั้นตอนปฏิบัติและการจัดเก็บผล
1. อ่าน Requirement/Source ของแต่ละ Feature และกำหนดรหัส `REQ-*` ภายในทีม
2. ลง Test Scenario และ Test Case ใน `test/test-cases/CPFriendFinder_TestCases.xlsx` ก่อนลงมือรัน
3. เขียน Unit Test ใน `src/test/java/com/cp/friend/service/` และรันเฉพาะคลาสที่เพิ่ม
4. เก็บหลักฐานคำสั่ง, เวลา, Branch, Commit SHA, Tests run, Failures, Errors, Skipped ใน `test/evidence/` เมื่อรันจริง
5. อัปเดต Actual Result/Status ใน Excel และสรุป `test/reports/test-summary.md`
6. เมื่อตรวจพบปัญหา ให้บันทึก Steps to Reproduce, Expected/Actual, Environment และสถานะใน Defect Summary; ส่งเรื่องให้เจ้าของ Feature
7. เมื่อ Backend Merge แล้ว ดึง `develop` เข้าสู่ branch Tester (หลังตรวจความขัดแย้ง) และรัน Regression Test

## 7. สิ่งส่งมอบของ Tester
- แผนการทดสอบนี้ + เอกสารคำอธิบายวิธีรัน
- Excel ที่มี Scenario, Test Case, Traceability, Defect Summary และ Test Summary Report
- ซอร์ส JUnit 5 + Mockito และผลทดสอบจริง
- หลักฐานรันคำสั่งและบันทึก defect เมื่อพบ

## 8. อ้างอิง
- ใบงานโปรเจกต์วิชา CP353002 (Spring Boot) ที่ทีมได้รับ
- โครง Template Lab #2 ที่ผู้ทดสอบส่งให้: Scenario Summary, Defect Summary, Scenario Details, Test Case Design & Results, Requirements Traceability, Test Summary Report
- ซอร์ส `AuthService`, `AuthController`, `RegisterRequest`, `LoginRequest`, `UserRepository`, `User` ใน Backend branch ณ วันที่ออกแบบ
