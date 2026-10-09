# CPFriendFinder - Test Workspace

เอกสารชุดนี้เป็นโครงเริ่มต้นของงานทดสอบรอบใหม่บน branch `develop`

- `test/test-plan/test-plan.md` แผนการทดสอบ
- `test/test-cases/CPFriendFinder_TestCases.xlsx` Test design และพื้นที่บันทึกผล
- Test code / report จะเพิ่มทีละ scenario หลังรันจริง

หลักการ:
- ทดสอบตาม behavior/module ไม่สร้าง unit test แยกให้ทุก Model ที่ไม่มี logic
- Mock dependency ภายนอกใน unit test (Repository, PasswordEncoder, Storage/S3)
- ไม่ใช้ secret หรือ credential จริงใน test code/เอกสาร
- Expected Result ออกแบบก่อนรัน; Actual Result/Pass/Fail บันทึกหลังรันจริง
- TS006 รอ Frontend กลับเข้า `develop`
