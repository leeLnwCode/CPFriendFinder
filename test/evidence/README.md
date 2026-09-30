# หลักฐานการรัน Test

บันทึก Log ของการรันจริงแต่ละรอบ เช่น `2026-09-30-auth-register-output.txt` โดยควรมีวันที่/เวลา, branch, commit SHA, คำสั่งที่ใช้, Tests run / Failures / Errors / Skipped และรายละเอียด error (ถ้ามี)

ตัวอย่างการเก็บ Log บน Git Bash (รันจริงเท่านั้น):

```bash
git branch --show-current
git rev-parse --short HEAD
./mvnw -Djava.version=21 -Dtest=AuthServiceRegisterTest test 2>&1 | tee test/evidence/auth-register-output.txt
```

บันทึกเฉพาะหลักฐานที่จำเป็นและไม่มีข้อมูลลับ เช่น credential ของฐานข้อมูล; อย่า Commit ทั้ง `target/` และอย่าใช้ Log เก่ามาอ้างว่า Test ใหม่ผ่าน
