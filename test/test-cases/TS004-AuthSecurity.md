# TS004 — Authentication API และ Spring Security

**ระดับการทดสอบ:** Spring MVC Slice Test (`@WebMvcTest` + `SecurityConfig`)

**ขอบเขต:** ตรวจสอบ HTTP Request/Response และ Session โดยใช้ SecurityFilterChain จริงและ Mock AuthService ไม่เชื่อมต่อฐานข้อมูลจริง

| Test Case ID | กรณีทดสอบ | ผลที่คาดหวัง | สถานะ |
|---|---|---|---|
| SEC-001 | สมัครสมาชิกโดยไม่เข้าสู่ระบบและไม่ส่ง CSRF Token | 201; JSON ไม่มีรหัสผ่าน | No run |
| SEC-002 | เข้าสู่ระบบโดยไม่ส่ง CSRF Token | 200; เก็บ `userId` ใน Session | No run |
| SEC-003 | เรียก `GET /api/auth` โดยไม่มี Session ของผู้ใช้ | 200; `userId` เป็น null | No run |
| SEC-004 | เข้าสู่ระบบแล้วตรวจ Session ด้วย Request ถัดไป | 200; `userId` ตรงกับผู้ใช้ | No run |
| SEC-005 | ออกจากระบบเมื่อมี Session โดยไม่ส่ง CSRF Token | 204; Session ถูกยกเลิก | No run |
| SEC-006 | สมัครสมาชิกด้วย JSON ไม่สมบูรณ์ | 400 | No run |
| SEC-007 | เข้าสู่ระบบโดยไม่ส่ง Request Body | 400 | No run |
| SEC-008 | ออกจากระบบโดยไม่เคยเข้าสู่ระบบและไม่ส่ง CSRF Token | 204 | No run |
| SEC-009 | เข้าสู่ระบบด้วย JSON ไม่สมบูรณ์ | 400 | No run |

**วิธีรัน:** `./mvnw -Djava.version=21 clean test -Dtest=AuthSecurityWebMvcTest`

บันทึก Actual Result และเปลี่ยนสถานะตามผลที่รันจริงเท่านั้น
