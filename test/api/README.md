# API Test (เตรียมพื้นที่)

ยังไม่มีผลทดสอบ API จริงในชุดเริ่มต้นนี้ เมื่อทีมยืนยัน Contract และ Security Configuration แล้ว ให้เพิ่ม Postman Collection หรือ Automated API Test สำหรับ `/api/auth/register`, `/api/auth/login`, `GET /api/auth`, `/api/auth/logout` พร้อม request, expected HTTP status, actual response และหลักฐาน

Endpoint ที่พบในซอร์ส `AuthController` ไม่เท่ากับการรับรองว่าใช้งานได้ครบทุกกรณี; สถานะยังเป็น Planned
