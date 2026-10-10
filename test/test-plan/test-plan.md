# แผนการทดสอบ CPFriendFinder

[หน้ารวมงานทดสอบ](../README.md) · [รายงานผลล่าสุด](../reports/test-report.md) · [Excel รายเคส](../test-cases/CPFriendFinder_TestCases.xlsx)

## วัตถุประสงค์

ตรวจความถูกต้องของ backend, frontend, API, สิทธิ์เข้าถึง การเก็บข้อมูล และการใช้งานผ่านเบราว์เซอร์ พร้อมบันทึก Expected Result, Actual Result และสถานะตามผลจริงใน Excel เดิม

## ขอบเขต

| สถานการณ์ | ระบบที่ตรวจ |
|---|---|
| TS001 | สมัครสมาชิกและตรวจข้อมูลซ้ำ/ข้อมูลไม่ถูกต้อง |
| TS002 | เข้าสู่ระบบและตรวจรหัสผ่าน |
| TS003 | Authentication API, validation และ session |
| TS004 | Spring Security และสิทธิ์เข้าถึง endpoint |
| TS005 | Interest Service / API |
| TS006 | Web flow, frontend authentication และ static resources |
| TS007 | โปรไฟล์ผู้ใช้ |
| TS008 | ความสนใจของผู้ใช้ |
| TS009 | ส่ง รับ ปฏิเสธ และยกเลิกคำขอเป็นเพื่อน |
| TS010 | ความเป็นเพื่อนและค้นหาผู้ใช้ |
| TS011 | จับคู่และคำนวณความสนใจร่วม |
| TS012 | การแจ้งเตือน |
| TS013 | ห้องสนทนาและสมาชิก |
| TS014 | ข้อความสนทนา |
| TS015 | Frontend contract: เพื่อน แจ้งเตือน และแชท |
| TS016 | WebSocket security, WebRTC และ call signaling |
| TS017 | Robot Framework Browser E2E |

เคส regression ที่เพิ่มและยังใช้รหัส `JUnit-20261010` ใน Excel แสดงแยกในรายงานเพื่อรักษารหัสเดิมและนับครบ 240 เคส

## ระดับและวิธีทดสอบ

| ระดับ | เครื่องมือ / วิธี |
|---|---|
| Unit | JUnit 5 + Mockito; mock Repository, PasswordEncoder และ Storage ตามขอบเขตเคส |
| MVC / Security | MockMvc; ตรวจ HTTP status, validation, session และสิทธิ์เข้าถึง |
| Integration / Persistence | Spring Boot Test / H2; ตรวจ schema และการเก็บ UserProfile |
| Frontend contract | ตรวจข้อตกลงระหว่าง JavaScript, template และ API |
| Web resources | HTTP smoke: หน้าเว็บและ CSS/JS/image references |
| Browser E2E | Robot Framework Browser กับเว็บ Railway |
| Manual | สองบัญชี/สองเครื่องตามกรณีเพื่อน แชท realtime คอล แชร์หน้าจอ และ session |

## ข้อมูลทดสอบ

JUnit ใช้ข้อมูลจำลองและฐานข้อมูลทดสอบตามแต่ละชุด Robot สร้างบัญชีด้วยอีเมลรูปแบบ `robot.<สุ่ม>@example.com` และนามสกุล Tester ชุดเต็มสร้างบัญชีทดสอบใหม่ 3 บัญชีต่อรอบ และแก้เฉพาะข้อมูลบัญชีที่สร้าง ส่วน Manual ใช้บัญชีที่ผู้ทดสอบเตรียมไว้

## การดำเนินการ

1. รัน JUnit จาก root โปรเจกต์และเก็บ XML ใน `target/surefire-reports/`
2. รัน Robot ตาม [คู่มือ](../e2e/README.md) โดยใช้โฟลเดอร์ผลใหม่ต่อรอบ
3. ตรวจ WEB-001–WEB-005 โดยบันทึกแหล่งผล Robot หรือ HTTP smoke ของแต่ละเคส
4. ทดสอบ Manual ตาม [ขั้นตอน MAN-001–MAN-019](../reports/test-report.md#ผล-manual-รายเคส)
5. เติมผลลง [Excel](../test-cases/CPFriendFinder_TestCases.xlsx) โดยคงรูปแบบและประวัติเดิม
6. อัปเดต [รายงาน](../reports/test-report.md) และ [JSON](../reports/test-results.json) ให้ตรงกับผลรอบเดียวกัน

## ผลที่บันทึกล่าสุด

วันที่ 10 ตุลาคม 2026: JUnit 240/240, Robot 15/15 และ Manual ตามผู้ทดสอบรายงาน 19/19 ผ่าน รวม 274 ผล โดย HTTP static resources 52/52 แสดงแยก ไม่บวกซ้ำ รายละเอียดแต่ละ TS และรายเคสอยู่ใน [รายงานล่าสุด](../reports/test-report.md)

## เกณฑ์การบันทึกและสรุป

สถานะ Pass บันทึกเมื่อผลตรง Expected Result; Fail เมื่อผลไม่ตรง; Not Run เมื่อยังไม่รัน; Blocked เมื่อมีเงื่อนไขขัดขวาง แยก Error และ Skipped ตามผลจากเครื่องมือ หากมี defect ให้เชื่อมโยงรหัส defect กับเคสที่เกี่ยวข้อง ใช้ผลสุดท้ายของแต่ละ execution สรุปรอบนั้น และเก็บรอบก่อนหน้าไว้เป็นประวัติ
