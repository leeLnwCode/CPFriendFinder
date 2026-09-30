# เอกสารการทดสอบ CPFriendFinder

เอกสารชุดนี้ประกอบด้วยแผนการทดสอบ กรณีทดสอบ และผลการรัน JUnit สำหรับฟังก์ชันสมัครสมาชิกและเข้าสู่ระบบ

| ตำแหน่ง | รายละเอียด |
|---|---|
| `test-plan/test-plan.md` | ขอบเขต วิธีทดสอบ และเกณฑ์ผ่าน |
| `test-cases/CPFriendFinder_TestCases.xlsx` | Test Scenario, Test Case, Traceability และ Test Summary |
| `reports/baseline-2026-09-30.md` | ผลทดสอบเริ่มต้น |
| `reports/auth-tests-2026-09-30.md` | บันทึกผล Maven รอบล่าสุด |
| `reports/test-summary.md` | รายงานผลการทดสอบ |
| `reports/review-notes.md` | ข้อสังเกตจากการตรวจโค้ด |
| `evidence/` | หลักฐานการรันทดสอบเพิ่มเติม |
| `api/` | เอกสารและผลทดสอบ API |

**โค้ดทดสอบ:** `src/test/java/com/cp/friend/service/`

**คำสั่งรันทั้งหมด**

```bash
./mvnw -Djava.version=21 clean test
```

ผลวันที่ 30 กันยายน 2569: รันรวม 25 รายการ ผ่าน 25 รายการ (ข้อมูล Test Case ใน Excel ครอบคลุม Register 12 และ Login 11 กรณี)
