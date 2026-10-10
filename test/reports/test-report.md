# รายงานทดสอบ CPFriendFinder

วันที่สรุป: 10 ตุลาคม 2026 (Asia/Bangkok)
เว็บที่ทดสอบ: https://cpfriendfinder-final-production.up.railway.app
ผู้ทดสอบ manual: จิรัชญา เป้าจันทึก

## สรุปผล

| ชุดทดสอบ | จำนวน | ผ่าน | ไม่ผ่าน | Error | ข้าม/รอ |
|---|---:|---:|---:|---:|---:|
| JUnit 5 / Mockito / Spring Boot Test / H2 | 240 | 240 | 0 | 0 | 0 |
| Robot Framework Browser บน Railway | 15 | 15 | 0 | 0 | 0 |
| Manual ตามผลผู้ทดสอบรายงาน | 19 | 19 | 0 | — | 0 |

รวม 274 ผลจากการทดสอบคนละระดับ ไม่ใช่ 274 ฟังก์ชันที่ไม่ซ้ำกัน
HTTP resource smoke ของ WEB-004 ตรวจไฟล์คงที่ 52/52 ไฟล์ผ่าน แสดงแยกและไม่บวกเป็น E2E 52 เคส

## หลักฐานและรุ่นที่ทดสอบ

- JUnit: XML 37 ชุดจาก target/surefire-reports ในเครื่องผู้ทดสอบ รันผ่าน 240 เคสก่อนรับผล manual รอบนี้ ไม่ได้รัน JUnit ใหม่
- Robot: CPFriendFinder-E2E-20261010-182412.zip และ output.xml รันจริงเวลา 18:24–18:25 น. ใช้เวลา 62.750 วินาที
- Manual: CPFriendFinder-manual-2026-10-10T12-03-09.734Z.json กรอกเวลา 18:58–19:02 น. ส่งออกเวลา 19:03:09 น.
- JSON manual ไม่ระบุรุ่นที่ deploy หรือสภาพแวดล้อม และช่องไฟล์หลักฐานทุกเคสว่าง บันทึก PASS ตามผลที่ผู้ทดสอบแจ้ง ไม่อ้างว่าได้ตรวจภาพหรือรันทดสอบซ้ำ
- develop ที่เคยตรวจอ้างอิง: 108d3d0ea2db424b7b78bca83335810491901f3c ไม่ใช้ค่านี้แทนรุ่น deploy ที่ไม่ได้ระบุ
- เก็บ SHA-256 ของหลักฐานและรายละเอียดรายเคสใน test-results.json

## TS006 Web Flow / Static Resources

| ID | ผล | หลักฐาน |
|---|---|---|
| WEB-001 หน้า login | PASS | E2E-001 |
| WEB-002 หน้า register | PASS | E2E-003 |
| WEB-003 หน้า home | PASS | E2E-004 และ E2E-006 |
| WEB-004 static resources | PASS | HTTP 200 ชนิดไฟล์ถูกต้องและเนื้อหาไม่ว่างครบ 52 ไฟล์ |
| WEB-005 auth/session flow | PASS | E2E-004/005/014/015 |

WEB-004 ตรวจ literal references ใน templates ไม่ครอบคลุมรูปอัปโหลด URL ที่สร้างตอน runtime หรือทรัพยากรภายนอก
E2E-015 ลบ cookie แล้วตรวจ API 401 ไม่ใช่การรอ idle timeout จริง ส่วน MAN-018 บันทึกผล manual แยกด้านล่าง

## ผล Robot E2E

| ID | รายการ | ผล | วินาที |
|---|---|---|---:|
| E2E-001 | หน้าเข้าสู่ระบบโหลดได้ | PASS | 1.065 |
| E2E-002 | แสดงและซ่อนรหัสผ่าน | PASS | 0.784 |
| E2E-003 | ลิงก์สมัครสมาชิก | PASS | 1.079 |
| E2E-004 | สมัครสมาชิก กรอกข้อมูลให้อัตโนมัติ และเข้าสู่ระบบ | PASS | 3.421 |
| E2E-005 | บันทึกความสนใจหลังสมัคร | PASS | 3.209 |
| E2E-006 | หน้าหลักและข้อมูลห้องจริง | PASS | 1.419 |
| E2E-007 | หน้าเพื่อนเมื่อยังไม่มีเพื่อน | PASS | 1.496 |
| E2E-008 | หน้าการแจ้งเตือน | PASS | 1.738 |
| E2E-009 | หน้าสุ่มคุยและข้อมูลผู้แนะนำ | PASS หลัง retry | 2.126 |
| E2E-010 | เปิดหน้าแก้ไขโปรไฟล์ | PASS | 2.270 |
| E2E-011 | บันทึกเกี่ยวกับฉันและโหลดซ้ำ | PASS | 6.007 |
| E2E-012 | ลบและเพิ่มความสนใจ | PASS | 8.772 |
| E2E-013 | หน้าจอมือถือไม่ล้นแนวนอน | PASS | 1.255 |
| E2E-014 | ออกจากระบบและยกเลิก session | PASS | 2.192 |
| E2E-015 | กลับหน้า login เมื่อ API ตอบ 401 หลังลบ cookie | PASS หลัง retry | 3.278 |

## ผล Manual รายเคส

| ID | ขั้นตอน/รายการ | ผลที่คาดหวัง | ผลจริงที่ผู้ทดสอบกรอก | สถานะ | เวลา (+07:00) | หลักฐานภาพ |
|---|---|---|---|---|---|---|
| MAN-001 | A ส่งคำขอเพื่อนให้ B; B เปิดแจ้งเตือน | คำขอขึ้น realtime และรับได้; สถานะทั้งสองฝั่งตรงกัน | เป็นไปตาม Expected result | PASS | 2026-10-10 18:58 | ไม่แนบ |
| MAN-002 | A/B เป็นเพื่อนแล้วส่งข้อความสลับกัน | อีกฝั่งเห็นทันทีและช่องส่งล้างทันที ไม่เด้งไปหน้า direct แยก | เป็นไปตาม Expected result | PASS | 2026-10-10 18:59 | ไม่แนบ |
| MAN-003 | รีโหลดและเข้าแชทเพื่อนเดิม | ประวัติยังอยู่ ไม่ซ้ำและไม่หายเมื่อมีข้อความสดเข้าระหว่างโหลด | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-004 | เจ้าของแก้/ลบข้อความ; อีกฝั่งดูพร้อมกัน | ทั้งสองฝั่งอัปเดต; อีกฝ่ายแก้/ลบข้อความของเราไม่ได้ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-005 | ส่งรูปในแชทเพื่อน แล้วรีโหลด | รูปถูกเก็บและโหลดได้จริงจาก storage | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-006 | สร้าง/แก้/ลบห้องด้วยบัญชีเจ้าของ | ข้อมูลและรายการห้องเปลี่ยน; สมาชิกทั่วไปจัดการห้องไม่ได้ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-007 | A เปิด home, B เข้าหรือออกห้อง | จำนวนและรายชื่ออัปเดตโดยไม่ต้องรีโหลดหรือเข้าคอล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-008 | ปิดเว็บแล้วกลับเข้าห้องที่ยังเป็นสมาชิก | เข้าต่อได้ ไม่มี Already a member และไม่เพิ่มสมาชิกซ้ำ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-009 | สองบัญชีเข้าคอลในห้อง | ได้ยินสองทาง จำนวนคนไม่คูณสอง และโปรไฟล์ไม่ซ้ำ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 | ไม่แนบ |
| MAN-010 | A โทรเสียงหา B จากหน้าเพื่อน; รับ/ปฏิเสธ/ยกเลิก | มีสายเข้า; เชื่อมต่อสำเร็จหรือปิดสายตรงสถานะ ไม่มี popup ค้าง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-011 | B อยู่หน้าอื่นแล้วรับสายของ A | ไปหน้าเพื่อนและต่อสายเดิมได้; ตรวจ membership ห้องเดิมแยกจากสถานะคอล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-012 | เปลี่ยนเสียงเป็นวิดีโอและเปิด/ปิดกล้องสองฝั่ง | ภาพอัปเดตจริง เสียงไม่ขาด และอีกฝั่งเห็นสถานะกล้อง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-013 | สองเครื่องต่างเครือข่ายโทรหาเพื่อนและคอลห้อง | เสียง/ภาพถึงกัน; ถ้าทดสอบ TURN ต้องยืนยัน selected pair เป็น relay | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-014 | แชร์หน้าจอ/แท็บหลายคนและเลือกจอที่ดู | ผู้รับเห็นภาพจริง; จัด grid ได้และหยุดแชร์คืนกล้องถูกต้อง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-015 | ส่งรูปในห้องกลุ่ม แล้วออกและเปิดใหม่ | เห็นรูปสด; ไม่มีประวัติข้อความกลุ่มตามข้อตกลง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 | ไม่แนบ |
| MAN-016 | แก้รูปโปรไฟล์/เพิ่ม gallery สูงสุด 5 รูป; เปิดสุ่มคุยอีกบัญชี | รูปอยู่ในกรอบ, เลื่อนดูได้, ไม่เกิน 5 รูป, ไม่มี gallery ใช้ avatar | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 | ไม่แนบ |
| MAN-017 | เลือกปี/สาขา/ความสนใจแล้วสุ่มคุย | ผู้แนะนำเป็นบัญชีจริง; คะแนน/ความสนใจร่วมและตัวกรองตรงข้อมูล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 | ไม่แนบ |
| MAN-018 | ปล่อย session หมดอายุจริงโดยไม่เรียก API | กลับ login; ทดสอบทั้งหน้าเปิดค้างและกลับมาจากแท็บพื้นหลัง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 | ไม่แนบ |
| MAN-019 | จำลองเครือข่ายขาด/HTTP 503 | แจ้งข้อผิดพลาดและลองใหม่ได้ ไม่ถือเป็น session หมดอายุโดยอัตโนมัติ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 | ไม่แนบ |

## ข้อสังเกตและข้อจำกัด

- E2E-009 และ E2E-015 มี assertion ไม่ผ่านชั่วคราวระหว่างโหลด แล้วผ่านหลัง retry ภายในเวลาที่กำหนด สถานะสุดท้ายเป็น PASS
- Manual ทั้ง 19 เคสบันทึก Actual Result ว่า “เป็นไปตาม Expected result” จึงเก็บข้อความตามต้นทาง ไม่เพิ่มรายละเอียดการสังเกตที่ผู้ทดสอบไม่ได้ระบุ
- MAN-013 รายงาน PASS แต่ไม่แนบ selected candidate pair จึงไม่มีหลักฐานเทคนิคยืนยัน relay ในชุดเอกสารนี้
- MAN-018 รายงาน PASS แต่ไม่ระบุ timeout ที่ตั้งไว้ ระยะ idle หรือ Network log จึงไม่อนุมานรายละเอียดเวลา
- ไม่มี FAIL หรือ BLOCKED ในผลรอบที่ได้รับ ไม่ได้หมายความว่าระบบไม่มีข้อบกพร่องในเงื่อนไขที่ยังไม่ครอบคลุม
- ไม่ใช้ผล dry-run หรือการจำลอง signaling มานับเป็นผลผ่านบนเว็บจริง

## Excel และประวัติ

อัปเดต Actual Result, Status และ Execution Reference ของ MAN-001 ถึง MAN-019
เพิ่ม Execution Log และสรุปรอบ manual แยกจาก JUnit/Robot เก็บประวัติรอบเดิมไว้
คงชื่อ/ลำดับชีต สูตร รูปแบบ และข้อมูลนอกส่วนที่เติม ไม่มีการเปลี่ยนโค้ดระบบหรือใช้ Git

## ผล JUnit แยกชุดทดสอบ

| ชุดทดสอบ | จำนวน | Fail | Error | Skipped |
|---|---:|---:|---:|---:|
| com.cp.friend.config.AuthSecurityWebMvcTest | 10 | 0 | 0 | 0 |
| com.cp.friend.config.SessionHandshakeInterceptorTest | 2 | 0 | 0 | 0 |
| com.cp.friend.config.WebSocketAuthChannelInterceptorTest | 10 | 0 | 0 | 0 |
| com.cp.friend.controller.AuthControllerMvcTest | 10 | 0 | 0 | 0 |
| com.cp.friend.controller.ChatMessageControllerMvcTest | 2 | 0 | 0 | 0 |
| com.cp.friend.controller.ChatRoomControllerMvcTest | 1 | 0 | 0 | 0 |
| com.cp.friend.controller.FriendRequestControllerMvcTest | 2 | 0 | 0 | 0 |
| com.cp.friend.controller.FriendshipControllerMvcTest | 2 | 0 | 0 | 0 |
| com.cp.friend.controller.InterestControllerMvcTest | 8 | 0 | 0 | 0 |
| com.cp.friend.controller.NotificationControllerMvcTest | 1 | 0 | 0 | 0 |
| com.cp.friend.controller.PageControllerMvcTest | 3 | 0 | 0 | 0 |
| com.cp.friend.controller.ProfileSummaryControllerTest | 6 | 0 | 0 | 0 |
| com.cp.friend.controller.UserControllerMvcTest | 5 | 0 | 0 | 0 |
| com.cp.friend.controller.UserDiscoveryControllerMvcTest | 1 | 0 | 0 | 0 |
| com.cp.friend.controller.UserInterestControllerMvcTest | 8 | 0 | 0 | 0 |
| com.cp.friend.FriendApplicationTests | 1 | 0 | 0 | 0 |
| com.cp.friend.frontend.CallFrontendContractTest | 10 | 0 | 0 | 0 |
| com.cp.friend.frontend.FrontendAuthContractTest | 6 | 0 | 0 | 0 |
| com.cp.friend.integration.FriendNotificationChatContractTest | 8 | 0 | 0 | 0 |
| com.cp.friend.schema.ProfilePersistenceTest | 1 | 0 | 0 | 0 |
| com.cp.friend.schema.SchemaExportTest | 1 | 0 | 0 | 0 |
| com.cp.friend.service.AuthServiceLoginTest | 11 | 0 | 0 | 0 |
| com.cp.friend.service.AuthServiceRegisterTest | 15 | 0 | 0 | 0 |
| com.cp.friend.service.ChatMessageServiceTest | 19 | 0 | 0 | 0 |
| com.cp.friend.service.ChatRoomServiceTest | 19 | 0 | 0 | 0 |
| com.cp.friend.service.FriendRequestServiceTest | 13 | 0 | 0 | 0 |
| com.cp.friend.service.FriendshipServiceTest | 4 | 0 | 0 | 0 |
| com.cp.friend.service.ImageContentStrategyTest | 3 | 0 | 0 | 0 |
| com.cp.friend.service.InterestServiceTest | 5 | 0 | 0 | 0 |
| com.cp.friend.service.MatchingServiceTest | 4 | 0 | 0 | 0 |
| com.cp.friend.service.NotificationServiceTest | 9 | 0 | 0 | 0 |
| com.cp.friend.service.ProfileGalleryTest | 9 | 0 | 0 | 0 |
| com.cp.friend.service.RoomRealtimeServiceTest | 6 | 0 | 0 | 0 |
| com.cp.friend.service.UserDiscoveryServiceTest | 5 | 0 | 0 | 0 |
| com.cp.friend.service.UserInterestServiceTest | 7 | 0 | 0 | 0 |
| com.cp.friend.service.UserServiceTest | 7 | 0 | 0 | 0 |
| com.cp.friend.strategy.InterestMatchingStrategyTest | 6 | 0 | 0 | 0 |
