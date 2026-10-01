# CPFriendFinder - SQA Test Workspace

โครงสร้างเริ่มต้นสำหรับงานทดสอบรอบใหม่ของโปรเจกต์ CPFriendFinder

- `test/test-plan/test-plan.md` แผนการทดสอบ
- `test/test-cases/CPFriendFinder_TestCases.xlsx` Test Case Workbook
- Test code จะเพิ่มทีละ scenario ใน commit ถัดไป
- ผลการทดสอบจริงจะบันทึกหลังจากรันกับโค้ดใน `develop` ของรอบนั้น

แนวทาง:
1. ทดสอบตามพฤติกรรม/โมดูล ไม่ไล่เขียน test ให้ทุกไฟล์ Model
2. แยก Scenario ใน Excel เป็นคนละ Sheet แต่เก็บใน Workbook เดียว
3. Frontend/Web Flow รอจน Frontend ถูกนำกลับเข้า `develop`
4. ไม่บันทึก secret หรือ credential จริงใน test code / test document
