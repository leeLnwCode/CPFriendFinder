# แผนการทดสอบ CPFriendFinder

**รายวิชา:** CP353002 Principles of Software Design and Development  
**ผู้รับผิดชอบ:** จิรัชญา เป้าจันทึก (Tester)  
**วันที่จัดทำ:** 30 กันยายน 2569

## 1. วัตถุประสงค์

ตรวจสอบความถูกต้องของฟังก์ชันและการทำงานของระบบ CPFriendFinder โดยเริ่มจากการทดสอบการสมัครสมาชิกและเข้าสู่ระบบ พร้อมจัดทำกรณีทดสอบ บันทึกผล และสรุปผลการทดสอบ

## 2. ขอบเขตการทดสอบ

| ส่วนที่ทดสอบ | วิธีทดสอบ | ผลล่าสุด |
|---|---|---|
| Register (`AuthService.register`) | Unit Test: JUnit 5 และ Mockito | ผ่าน 12/12 |
| Login (`AuthService.login`) | Unit Test: JUnit 5 และ Mockito | ผ่าน 11/11 |
| Spring Application Context | Spring Boot Test | ผ่าน 1/1 |
| `AuthServiceTest` ที่มีอยู่ | JUnit | ผ่าน 1/1 |

ผลรอบนี้ยังไม่ครอบคลุม Controller, HTTP API, การเชื่อมต่อฐานข้อมูลจริง หรือ UAT

## 3. การออกแบบ Test Case

- **Equivalence Class:** ข้อมูลถูกต้อง ข้อมูลว่าง ค่า null อีเมลซ้ำ รหัสผ่านผิด และสถานะบัญชี
- **Boundary Value Analysis:** ความยาวรหัสผ่าน 7 และ 8 ตัวอักษรตามเงื่อนไขขั้นต่ำ
- **Unit Test:** ใช้ Mockito จำลอง `UserRepository` และ `PasswordEncoder` เพื่อทดสอบตรรกะภายใน `AuthService` แยกจากฐานข้อมูล

รายการ Test Case และความสัมพันธ์กับข้อกำหนดอยู่ใน `test/test-cases/CPFriendFinder_TestCases.xlsx`

## 4. สภาพแวดล้อม

| รายการ | รายละเอียด |
|---|---|
| ระบบปฏิบัติการ | Windows 11 |
| Terminal | Git Bash |
| Java | Eclipse Temurin 21.0.11 |
| Maven Wrapper | Apache Maven 3.9.16 |
| คำสั่งที่ใช้ | `./mvnw -Djava.version=21 clean test` |
| ฐานข้อมูล | จำลอง Repository ด้วย Mockito สำหรับ Unit Test |

## 5. เกณฑ์ผ่าน

แต่ละ Test Case ถือว่าผ่านเมื่อผลลัพธ์ตรงกับเงื่อนไขที่กำหนดและ JUnit Assertion ผ่าน โดยบันทึกผลจริงใน Excel และรายงานผลการรันของ Maven

## 6. ผลการรันทดสอบ

**30 กันยายน 2569 เวลา 14:14 น.**

| ชุดทดสอบ | รัน | ผ่าน | ไม่ผ่าน | Error | ข้าม |
|---|---:|---:|---:|---:|---:|
| FriendApplicationTests | 1 | 1 | 0 | 0 | 0 |
| AuthServiceRegisterTest | 12 | 12 | 0 | 0 | 0 |
| AuthServiceLoginTest | 11 | 11 | 0 | 0 | 0 |
| AuthServiceTest | 1 | 1 | 0 | 0 | 0 |
| **รวม** | **25** | **25** | **0** | **0** | **0** |

**ผล Maven:** `BUILD SUCCESS` (1 นาที 34 วินาที)

Test Case ใน Excel ครอบคลุม Register 12 กรณีและ Login 11 กรณี รวม 23 กรณี อีก 2 รายการเป็นการทดสอบเพิ่มเติมที่แสดงในผล Maven

## 7. การบันทึกผลและข้อบกพร่อง

- สถานะของ Test Case บันทึกใน Excel โดยอ้างอิงการรันจริง
- ผลการรันรายคลาสดูได้ใน `target/surefire-reports/` ของเครื่องที่รันทดสอบ
- หากพบกรณีไม่ผ่าน ให้บันทึกขั้นตอนทำซ้ำ ผลที่คาดหวัง ผลจริง และข้อมูลที่จำเป็นใน `Defect Summary`

## 8. เอกสารอ้างอิง

- ใบงานโปรเจกต์ CP353002
- รูปแบบ Test Scenario/Test Case จาก Lab 2 วิชา CP353201 Software Quality Assurance
- โค้ด `AuthService`, `AuthController`, DTO, `UserRepository` และ `User` ของ CPFriendFinder ที่ใช้เป็นฐานในการออกแบบชุดทดสอบ
