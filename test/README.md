# งานทดสอบ CPFriendFinder

อัปเดตผลวันที่ **10 ตุลาคม 2026** ผู้ทดสอบ: **จิรัชญา เป้าจันทึก**

ทดสอบ backend, frontend และการใช้งานบน [เว็บ Railway](https://cpfriendfinder-final-production.up.railway.app) ครอบคลุมสถานการณ์ TS001–TS017 และ Manual MAN-001–MAN-019

## ผลรอบล่าสุด

| ระดับการทดสอบ | ทั้งหมด | ผ่าน | ไม่ผ่าน | Error | ข้าม |
|---|---:|---:|---:|---:|---:|
| JUnit 5 / Mockito / Spring Boot Test / H2 | 240 | 240 | 0 | 0 | 0 |
| Robot Framework Browser บนเว็บ Railway | 15 | 15 | 0 | 0 | 0 |
| Manual ตามผลผู้ทดสอบรายงาน | 19 | 19 | 0 | — | 0 |

รวม **274/274 ผลผ่าน** จากการทดสอบคนละระดับ ส่วน WEB-004 ตรวจ static resources ผ่าน **52/52 ไฟล์** แสดงแยกจากยอด 274 เพื่อไม่บวกซ้ำ ผลเก่ายังคงอยู่ใน Excel โดยดูรอบวันที่ 10 ตุลาคม 2026 เป็นผลล่าสุด

## เปิดเอกสารและไฟล์ทดสอบ

| ไฟล์ | เนื้อหา |
|---|---|
| [รายงานทดสอบ](reports/test-report.md) | สรุป TS001–TS017, JUnit 240 รายเคส, Robot 15 และ Manual 19 รายเคส อ่านบน GitHub ได้ |
| [รายงาน HTML](reports/test-report.html) | ดาวน์โหลดแล้วเปิดในเบราว์เซอร์สำหรับอ่านหรือนำเสนอ |
| [Excel: Test Cases และผลทดสอบ](test-cases/CPFriendFinder_TestCases.xlsx) | Test design, Expected/Actual Result, สถานะ, execution log และประวัติ |
| [ข้อมูลผล JSON](reports/test-results.json) | รายละเอียดผล เวลา และข้อมูลอ้างอิงสำหรับตรวจย้อนหลัง |
| [แผนทดสอบ](test-plan/test-plan.md) | วัตถุประสงค์ ขอบเขต TS001–TS017 วิธีทดสอบและเก็บผล |
| [วิธีเตรียมเครื่องและรัน Robot](e2e/README.md) | ติดตั้งเครื่องมือ รัน smoke / full suite และอ่านผล |
| [JUnit test source](../src/test/java/) | Unit, MVC, Security, integration, persistence และ frontend contract |
| [Robot test source](e2e/robot/tests/) | ชุด E2E-001–E2E-015 |
| [Robot shared keywords](e2e/robot/resources/common.resource) | การเปิดเบราว์เซอร์ อ่าน API และเก็บผลเมื่อทดสอบไม่ผ่าน |

## รัน JUnit

รันจาก root โปรเจกต์ใน Git Bash โดยใช้ Java 21:

```bash
./mvnw.cmd test
```

ผล JUnit สร้างใน `target/surefire-reports/` ใช้ XML บันทึกจำนวน Tests / Failures / Errors / Skipped และผลราย method

## รัน Robot

ดู [คู่มือ Robot](e2e/README.md) ผลรอบที่บันทึกล่าสุดคือ `full-20261010-182412` เวลา 18:24–18:25 น. ผ่าน 15/15 เคส

## ผล Manual

MAN-001–MAN-019 ครอบคลุมเพื่อน แชท รูปภาพ ห้อง realtime เสียง/วิดีโอ แชร์หน้าจอ gallery matching และ session ผลตามที่ผู้ทดสอบกรอกเวลา 18:58–19:02 น. ผ่าน 19/19 เคส ดูขั้นตอน Expected Result และ Actual Result ใน [รายงานรายเคส](reports/test-report.md#ผล-manual-รายเคส) และ [Excel](test-cases/CPFriendFinder_TestCases.xlsx)

## การบันทึกผล

แยกผล JUnit, Robot และ Manual ตามแหล่งที่มา กรอก Actual Result และสถานะตามผลจริง เก็บประวัติรอบเดิมใน Excel ส่วน dry-run ใช้ตรวจโครงสร้างและ keyword เท่านั้น
