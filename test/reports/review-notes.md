# CPFriendFinder - Code Review Notes

| Field | Detail |
|:--|:--|
| Date | 2026-09-30 |
| Reviewed scope | Authentication, Web Flow, request validation and configuration |
| Latest verified local tests | 51/51 Pass |

ข้อสังเกตต่อไปนี้ได้จากการตรวจ Source Code และขอบเขตการทดสอบ **ไม่ใช่รายการ Fail ที่พบจาก 51 Tests** และยังไม่ถือเป็น Defect ที่ยืนยันจาก End-to-End Test

| ID | Observation | Detail | Follow-up |
|:--:|:--|:--|:--|
| OBS-01 | Java environment | `pom.xml` ใน baseline ที่ตรวจระบุ Java 21; การรันล่าสุดใช้ JDK 21 | ใช้ JDK/Build configuration ให้ตรงกันในทีม |
| OBS-02 | API validation | `AuthService` ตรวจ input ด้วยเงื่อนไขใน Method; `RegisterRequest` ไม่มี Bean Validation annotations และ `AuthController` ยังไม่ใช้ `@Valid` | ให้ Backend กำหนด Validation และรูปแบบ Error Response ก่อนเพิ่ม Test |
| OBS-03 | Session vs. Spring Security | API Login เก็บ `userId` ใน `HttpSession` แต่ไม่ได้สร้าง Spring Security `Authentication`; WEB-003 ตรวจเฉพาะ Anonymous `/home` | ทดสอบ Login → `/home` หลังทีมกำหนดวิธี Authentication |
| OBS-04 | Browser submission | Frontend Register/Login ยังไม่ได้เชื่อม API ใน baseline ที่ตรวจ โดยทีม Frontend กำลังแก้ | ทดสอบ Browser Flow หลัง Merge โค้ดใหม่ |
| OBS-05 | Exposed database credential | พบ Database credential ในไฟล์ Configuration ที่ถูก Track ใน Public Repository; ไม่บันทึกค่ารหัสผ่านในรายงาน | เจ้าของ Backend/Repository ต้อง Rotate Secret, ใช้ Environment Variables และตรวจ Git History |

**Security note:** การลบรหัสผ่านออกจากไฟล์ปัจจุบันไม่เพียงพอ ต้องยกเลิก Secret เก่าด้วย และควรให้ทีมจัดการก่อนนำระบบขึ้นใช้งานจริง
