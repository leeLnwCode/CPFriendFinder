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

## ข้อมูลการทดสอบ

- JUnit: XML 37 ชุดจาก target/surefire-reports ในเครื่องผู้ทดสอบ รันผ่าน 240 เคสก่อนรับผล manual รอบนี้ ไม่ได้รัน JUnit ใหม่
- Robot: CPFriendFinder-E2E-20261010-182412.zip และ output.xml รันจริงเวลา 18:24–18:25 น. ใช้เวลา 62.750 วินาที
- Manual: CPFriendFinder-manual-2026-10-10T12-03-09.734Z.json กรอกเวลา 18:58–19:02 น. ส่งออกเวลา 19:03:09 น.
- เก็บ SHA-256 ของหลักฐานและรายละเอียดรายเคสใน test-results.json

## สรุปผลครบทุกสถานการณ์ TS001–TS017

ตารางนี้ใช้ผลรอบล่าสุดวันที่ 10 ตุลาคม 2026 โดยรวม TS ที่มีรหัสลงท้าย `-R` ไว้กับ TS เดียวกัน แยก JUnit, Robot, Manual และ Web checks เพื่อแสดงที่มาของผลแต่ละส่วน

| TS | สถานการณ์ | JUnit ผ่าน/ทั้งหมด | Robot ผ่าน/ทั้งหมด | Manual ผ่าน/ทั้งหมด | Web checks |
|---|---|---:|---:|---:|---|
| TS001 | สมัครสมาชิก | 13/13 | — | — | — |
| TS002 | เข้าสู่ระบบ | 11/11 | — | — | — |
| TS003 | Authentication API / Validation / Session | 10/10 | — | — | — |
| TS004 | Spring Security และสิทธิ์เข้าถึง | 10/10 | — | — | — |
| TS005 | จัดการความสนใจและ Interest API | 13/13 | — | — | — |
| TS006 | Web Flow / Static Resources / Frontend Authentication | 8/8 | — | 2/2 | 5/5 (WEB-001–005) |
| TS007 | โปรไฟล์ผู้ใช้ | 12/12 | — | 1/1 | — |
| TS008 | ความสนใจของผู้ใช้ | 15/15 | — | — | — |
| TS009 | คำขอเป็นเพื่อน | 15/15 | — | 1/1 | — |
| TS010 | เพื่อนและค้นหาผู้ใช้ | 12/12 | — | — | — |
| TS011 | จับคู่และคำนวณความสนใจร่วม | 10/10 | — | 1/1 | — |
| TS012 | การแจ้งเตือน | 10/10 | — | — | — |
| TS013 | ห้องสนทนา | 11/11 | — | 3/3 | — |
| TS014 | ข้อความสนทนา | 12/12 | — | 5/5 | — |
| TS015 | Frontend Contract: เพื่อน / แจ้งเตือน / แชท | 8/8 | — | — | — |
| TS016 | WebSocket Security / WebRTC / Call Signaling | 6/6 | — | 6/6 | — |
| TS017 | Robot Framework Browser E2E | — | 15/15 | — | — |
| JUnit เพิ่มเติม | เคส regression ที่เพิ่มและยังไม่ได้กำหนดรหัส TS ใน Excel | 64/64 | — | — | — |
| รวมผลรอบล่าสุด | | 240/240 | 15/15 | 19/19 | 5/5 แสดงแยก |

รวมผล JUnit + Robot + Manual เท่ากับ 274/274 ผ่าน โดย WEB-001/002/003/005 อ้างอิง Robot เดิม และ WEB-004 เป็น HTTP resource smoke 52/52 ไฟล์ จึงไม่บวก Web checks ซ้ำในยอด 274

### เคส Manual ที่อ้างอิงแต่ละ TS

| TS | เคส | ผล |
|---|---|---|
| TS006 | MAN-018, MAN-019 | PASS ตามผลผู้ทดสอบ |
| TS007 | MAN-016 | PASS ตามผลผู้ทดสอบ |
| TS009 | MAN-001 | PASS ตามผลผู้ทดสอบ |
| TS011 | MAN-017 | PASS ตามผลผู้ทดสอบ |
| TS013 | MAN-006, MAN-007, MAN-008 | PASS ตามผลผู้ทดสอบ |
| TS014 | MAN-002, MAN-003, MAN-004, MAN-005, MAN-015 | PASS ตามผลผู้ทดสอบ |
| TS016 | MAN-009, MAN-010, MAN-011, MAN-012, MAN-013, MAN-014 | PASS ตามผลผู้ทดสอบ |

## รายละเอียด WEB-001–WEB-005

| ID | ผล | หลักฐาน |
|---|---|---|
| WEB-001 หน้า login | PASS | E2E-001 |
| WEB-002 หน้า register | PASS | E2E-003 |
| WEB-003 หน้า home | PASS | E2E-004 และ E2E-006 |
| WEB-004 static resources | PASS | HTTP 200 ชนิดไฟล์ถูกต้องและเนื้อหาไม่ว่างครบ 52 ไฟล์ |
| WEB-005 auth/session flow | PASS | E2E-004/005/014/015 |


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

| ID | ขั้นตอน/รายการ | ผลที่คาดหวัง | ผลจริงที่ผู้ทดสอบกรอก | สถานะ | เวลา (+07:00) |
|---|---|---|---|---|---|
| MAN-001 | A ส่งคำขอเพื่อนให้ B; B เปิดแจ้งเตือน | คำขอขึ้น realtime และรับได้; สถานะทั้งสองฝั่งตรงกัน | เป็นไปตาม Expected result | PASS | 2026-10-10 18:58 |
| MAN-002 | A/B เป็นเพื่อนแล้วส่งข้อความสลับกัน | อีกฝั่งเห็นทันทีและช่องส่งล้างทันที ไม่เด้งไปหน้า direct แยก | เป็นไปตาม Expected result | PASS | 2026-10-10 18:59 |
| MAN-003 | รีโหลดและเข้าแชทเพื่อนเดิม | ประวัติยังอยู่ ไม่ซ้ำและไม่หายเมื่อมีข้อความสดเข้าระหว่างโหลด | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-004 | เจ้าของแก้/ลบข้อความ; อีกฝั่งดูพร้อมกัน | ทั้งสองฝั่งอัปเดต; อีกฝ่ายแก้/ลบข้อความของเราไม่ได้ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-005 | ส่งรูปในแชทเพื่อน แล้วรีโหลด | รูปถูกเก็บและโหลดได้จริงจาก storage | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-006 | สร้าง/แก้/ลบห้องด้วยบัญชีเจ้าของ | ข้อมูลและรายการห้องเปลี่ยน; สมาชิกทั่วไปจัดการห้องไม่ได้ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-007 | A เปิด home, B เข้าหรือออกห้อง | จำนวนและรายชื่ออัปเดตโดยไม่ต้องรีโหลดหรือเข้าคอล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-008 | ปิดเว็บแล้วกลับเข้าห้องที่ยังเป็นสมาชิก | เข้าต่อได้ ไม่มี Already a member และไม่เพิ่มสมาชิกซ้ำ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-009 | สองบัญชีเข้าคอลในห้อง | ได้ยินสองทาง จำนวนคนไม่คูณสอง และโปรไฟล์ไม่ซ้ำ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:00 |
| MAN-010 | A โทรเสียงหา B จากหน้าเพื่อน; รับ/ปฏิเสธ/ยกเลิก | มีสายเข้า; เชื่อมต่อสำเร็จหรือปิดสายตรงสถานะ ไม่มี popup ค้าง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-011 | B อยู่หน้าอื่นแล้วรับสายของ A | ไปหน้าเพื่อนและต่อสายเดิมได้; ตรวจ membership ห้องเดิมแยกจากสถานะคอล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-012 | เปลี่ยนเสียงเป็นวิดีโอและเปิด/ปิดกล้องสองฝั่ง | ภาพอัปเดตจริง เสียงไม่ขาด และอีกฝั่งเห็นสถานะกล้อง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-013 | สองเครื่องต่างเครือข่ายโทรหาเพื่อนและคอลห้อง | เสียง/ภาพถึงกัน; ถ้าทดสอบ TURN ต้องยืนยัน selected pair เป็น relay | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-014 | แชร์หน้าจอ/แท็บหลายคนและเลือกจอที่ดู | ผู้รับเห็นภาพจริง; จัด grid ได้และหยุดแชร์คืนกล้องถูกต้อง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-015 | ส่งรูปในห้องกลุ่ม แล้วออกและเปิดใหม่ | เห็นรูปสด; ไม่มีประวัติข้อความกลุ่มตามข้อตกลง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:01 |
| MAN-016 | แก้รูปโปรไฟล์/เพิ่ม gallery สูงสุด 5 รูป; เปิดสุ่มคุยอีกบัญชี | รูปอยู่ในกรอบ, เลื่อนดูได้, ไม่เกิน 5 รูป, ไม่มี gallery ใช้ avatar | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 |
| MAN-017 | เลือกปี/สาขา/ความสนใจแล้วสุ่มคุย | ผู้แนะนำเป็นบัญชีจริง; คะแนน/ความสนใจร่วมและตัวกรองตรงข้อมูล | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 |
| MAN-018 | ปล่อย session หมดอายุจริงโดยไม่เรียก API | กลับ login; ทดสอบทั้งหน้าเปิดค้างและกลับมาจากแท็บพื้นหลัง | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 |
| MAN-019 | จำลองเครือข่ายขาด/HTTP 503 | แจ้งข้อผิดพลาดและลองใหม่ได้ ไม่ถือเป็น session หมดอายุโดยอัตโนมัติ | เป็นไปตาม Expected result | PASS | 2026-10-10 19:02 |


## Excel และประวัติ

อัปเดต Actual Result, Status และ Execution Reference ของ MAN-001 ถึง MAN-019
เพิ่ม Execution Log และสรุปรอบ manual แยกจาก JUnit/Robot เก็บประวัติรอบเดิมไว้
คงชื่อ/ลำดับชีต สูตร รูปแบบ และข้อมูลนอกส่วนที่เติม คงประวัติการทดสอบรอบเดิมไว้

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


## ผล JUnit รายเคสครบ 240 เคส

| ลำดับ | TS ตาม Excel | Case ID | วิธีทดสอบ / Method | ผล |
|---:|---|---|---|---|
| 1 | TS004-R | SEC-R001 | AuthSecurityWebMvcTest.publicPages_areNotBlockedBySecurity | PASS |
| 2 | TS004-R | SEC-R002 | AuthSecurityWebMvcTest.apiLogin_withoutAuthentication_reachesControllerLayer | PASS |
| 3 | TS004-R | SEC-R004 | AuthSecurityWebMvcTest.websocketRoutes_areNotBlockedBySecurity | PASS |
| 4 | TS004-R | SEC-R007 | AuthSecurityWebMvcTest.unmatchedRoute_requiresAuthentication | PASS |
| 5 | TS004-R | SEC-R005 | AuthSecurityWebMvcTest.swaggerRoutes_areNotBlockedBySecurity | PASS |
| 6 | TS004-R | SEC-R006 | AuthSecurityWebMvcTest.staticResources_areNotBlockedBySecurity | PASS |
| 7 | TS004-R | SEC-R010 | AuthSecurityWebMvcTest.passwordEncoder_usesSecureHashing | PASS |
| 8 | TS004-R | SEC-R003 | AuthSecurityWebMvcTest.apiPost_doesNotRequireCsrfToken | PASS |
| 9 | TS004-R | SEC-R008 | AuthSecurityWebMvcTest.csrfFilter_isDisabled | PASS |
| 10 | TS004-R | SEC-R009 | AuthSecurityWebMvcTest.formLoginAndHttpBasic_areDisabled | PASS |
| 11 | JUnit-20261010 | JUNIT-011 | SessionHandshakeInterceptorTest.anonymousHandshakeIsRejectedBeforeSocketUpgrade | PASS |
| 12 | JUnit-20261010 | JUNIT-012 | SessionHandshakeInterceptorTest.authenticatedHttpSessionCanUpgrade | PASS |
| 13 | TS016 | WSA-001 | WebSocketAuthChannelInterceptorTest.connect_withAuthenticatedPrincipal_preservesPrincipal | PASS |
| 14 | JUnit-20261010 | JUNIT-014 | WebSocketAuthChannelInterceptorTest.subscribeRoomDiscoveryUpdates_isAllowed | PASS |
| 15 | JUnit-20261010 | JUNIT-015 | WebSocketAuthChannelInterceptorTest.sendToApplicationDestination_isAllowed | PASS |
| 16 | JUnit-20261010 | JUNIT-016 | WebSocketAuthChannelInterceptorTest.subscribeOtherUsersPrivateTopics_isDenied | PASS |
| 17 | JUnit-20261010 | JUNIT-017 | WebSocketAuthChannelInterceptorTest.subscribeOwnNotificationsAndCall_isAllowed | PASS |
| 18 | TS016 | WSA-002 | WebSocketAuthChannelInterceptorTest.connect_withoutAuthenticatedPrincipal_mustNotTrustLoginHeader | PASS |
| 19 | JUnit-20261010 | JUNIT-019 | WebSocketAuthChannelInterceptorTest.subscribeRoomTopics_requiresMembershipForEachTopic | PASS |
| 20 | JUnit-20261010 | JUNIT-020 | WebSocketAuthChannelInterceptorTest.subscribeRoomWithoutMembership_isDenied | PASS |
| 21 | JUnit-20261010 | JUNIT-021 | WebSocketAuthChannelInterceptorTest.sendDirectlyToBrokerTopic_isDenied | PASS |
| 22 | JUnit-20261010 | JUNIT-022 | WebSocketAuthChannelInterceptorTest.disconnectWithoutPrincipal_isAllowedForCleanup | PASS |
| 23 | TS003-R | CTRL-R007 | AuthControllerMvcTest.register_futureDateOfBirth_returnsValidationError | PASS |
| 24 | TS003-R | CTRL-R010 | AuthControllerMvcTest.register_duplicateEmail_returnsBadRequestResponse | PASS |
| 25 | TS003-R | CTRL-R005 | AuthControllerMvcTest.login_blankPassword_returnsValidationError | PASS |
| 26 | TS003-R | CTRL-R003 | AuthControllerMvcTest.logout_invalidatesSessionAndReturnsNoContent | PASS |
| 27 | TS003-R | CTRL-R006 | AuthControllerMvcTest.register_shortPassword_returnsValidationError | PASS |
| 28 | TS003-R | CTRL-R008 | AuthControllerMvcTest.malformedJson_returnsBadRequest | PASS |
| 29 | TS003-R | CTRL-R009 | AuthControllerMvcTest.login_serviceRejectsCredentials_returnsBadRequestResponse | PASS |
| 30 | TS003-R | CTRL-R004 | AuthControllerMvcTest.login_invalidEmail_returnsValidationError | PASS |
| 31 | TS003-R | CTRL-R002 | AuthControllerMvcTest.login_validRequest_returnsOkAndStoresSession | PASS |
| 32 | TS003-R | CTRL-R001 | AuthControllerMvcTest.register_validRequest_returnsCreated | PASS |
| 33 | TS014 | MSG-012 | ChatMessageControllerMvcTest.sendMessage_validationAndSession_contract | PASS |
| 34 | TS014 | MSG-011 | ChatMessageControllerMvcTest.getMessages_invalidBefore_returnsBadRequest | PASS |
| 35 | TS013 | ROOM-012 | ChatRoomControllerMvcTest.roomEndpoints_statusAndSession_contract | PASS |
| 36 | TS009 | FRQ-014 | FriendRequestControllerMvcTest.send_validRequest_returnsCreated | PASS |
| 37 | TS009 | FRQ-015 | FriendRequestControllerMvcTest.send_invalidInputOrSession_isRejected | PASS |
| 38 | TS010 | FRI-006 | FriendshipControllerMvcTest.unfriend_validRequest_returnsNoContent | PASS |
| 39 | TS010 | FRI-005 | FriendshipControllerMvcTest.listFriends_withSession_returnsOk | PASS |
| 40 | TS005-R | INT-R009 | InterestControllerMvcTest.createInterest_validRequest_returnsCreated | PASS |
| 41 | TS005-R | INT-R012 | InterestControllerMvcTest.createInterest_nameOver100Characters_returnsValidationError | PASS |
| 42 | TS005-R | INT-R006 | InterestControllerMvcTest.listInterests_withoutSearch_returnsAll | PASS |
| 43 | TS005-R | INT-R013 | InterestControllerMvcTest.createInterest_duplicate_returnsConflict | PASS |
| 44 | TS005-R | INT-R011 | InterestControllerMvcTest.createInterest_blankName_returnsValidationError | PASS |
| 45 | TS005-R | INT-R010 | InterestControllerMvcTest.createInterest_withoutSession_returnsUnauthorized | PASS |
| 46 | TS005-R | INT-R007 | InterestControllerMvcTest.listInterests_blankSearch_returnsAll | PASS |
| 47 | TS005-R | INT-R008 | InterestControllerMvcTest.listInterests_withSearch_returnsMatches | PASS |
| 48 | TS012 | NOT-010 | NotificationControllerMvcTest.notificationEndpoints_enforceStatusAndSession | PASS |
| 49 | TS006-R | WEB-R001 | PageControllerMvcTest.loginPage_mapsToLoginView | PASS |
| 50 | TS006-R | WEB-R002 | PageControllerMvcTest.registerPage_mapsToRegisterView | PASS |
| 51 | JUnit-20261010 | JUNIT-051 | PageControllerMvcTest.entry_redirectsToLogin | PASS |
| 52 | JUnit-20261010 | JUNIT-052 | ProfileSummaryControllerTest.incomingReturnsActualRequestId | PASS |
| 53 | JUnit-20261010 | JUNIT-053 | ProfileSummaryControllerTest.outgoingReturnsPending | PASS |
| 54 | JUnit-20261010 | JUNIT-054 | ProfileSummaryControllerTest.requiresSession | PASS |
| 55 | JUnit-20261010 | JUNIT-055 | ProfileSummaryControllerTest.loadsInterestsAndSelf | PASS |
| 56 | JUnit-20261010 | JUNIT-056 | ProfileSummaryControllerTest.inactiveProfileHidden | PASS |
| 57 | JUnit-20261010 | JUNIT-057 | ProfileSummaryControllerTest.existingFriendTakesPriority | PASS |
| 58 | TS007 | PRO-011 | UserControllerMvcTest.updateMe_futureDate_returnsValidationError | PASS |
| 59 | TS007 | PRO-010 | UserControllerMvcTest.updateMe_validRequest_returnsUpdatedProfile | PASS |
| 60 | TS007 | PRO-008 | UserControllerMvcTest.getMe_withSession_returnsProfile | PASS |
| 61 | TS007 | PRO-009 | UserControllerMvcTest.getMe_withoutSession_returnsUnauthorized | PASS |
| 62 | TS007 | PRO-012 | UserControllerMvcTest.updateMe_invalidImageBase64_returnsValidationError | PASS |
| 63 | TS010 | DISC-006 | UserDiscoveryControllerMvcTest.discover_filtersAndSession_areHandled | PASS |
| 64 | TS008 | UINT-014 | UserInterestControllerMvcTest.replaceInterests_withoutSession_returnsUnauthorized | PASS |
| 65 | TS008 | UINT-008 | UserInterestControllerMvcTest.getInterests_withSession_returnsInterestDtos | PASS |
| 66 | TS008 | UINT-010 | UserInterestControllerMvcTest.replaceInterests_validIds_returnsUpdatedList | PASS |
| 67 | TS008 | UINT-011 | UserInterestControllerMvcTest.replaceInterests_emptyList_returnsEmptyList | PASS |
| 68 | TS008 | UINT-013 | UserInterestControllerMvcTest.replaceInterests_nullItem_returnsValidationError | PASS |
| 69 | TS008 | UINT-012 | UserInterestControllerMvcTest.replaceInterests_nullInterestIds_returnsValidationError | PASS |
| 70 | TS008 | UINT-009 | UserInterestControllerMvcTest.getInterests_withoutSession_returnsUnauthorized | PASS |
| 71 | TS008 | UINT-015 | UserInterestControllerMvcTest.replaceInterests_invalidInterest_returnsBadRequest | PASS |
| 72 | JUnit-20261010 | JUNIT-072 | FriendApplicationTests.contextLoads | PASS |
| 73 | JUnit-20261010 | JUNIT-073 | CallFrontendContractTest.callJs_leaveSignal_notifiesUiThatCallEnded | PASS |
| 74 | TS016 | CALL-004 | CallFrontendContractTest.callJs_handlesIncomingMediaSignal | PASS |
| 75 | JUnit-20261010 | JUNIT-075 | CallFrontendContractTest.voiceCallEnd_cancelsPendingOutgoingCall | PASS |
| 76 | JUnit-20261010 | JUNIT-076 | CallFrontendContractTest.friendJs_handlesRemoteCallEndedEvent | PASS |
| 77 | JUnit-20261010 | JUNIT-077 | CallFrontendContractTest.videoCallEnd_cancelsPendingOutgoingCall | PASS |
| 78 | TS016 | CALL-003 | CallFrontendContractTest.cameraToggle_publishesMediaState | PASS |
| 79 | JUnit-20261010 | JUNIT-079 | CallFrontendContractTest.globalCall_acceptRedirectsToFriendWithPendingCall | PASS |
| 80 | JUnit-20261010 | JUNIT-080 | CallFrontendContractTest.authenticatedPages_loadGlobalIncomingCallSupport | PASS |
| 81 | JUnit-20261010 | JUNIT-081 | CallFrontendContractTest.callJs_doesNotContainHardcodedTurnCredentials | PASS |
| 82 | JUnit-20261010 | JUNIT-082 | CallFrontendContractTest.friendPage_resumesPendingIncomingCall | PASS |
| 83 | TS006-R | WEB-R005 | FrontendAuthContractTest.loginJavascript_storesUserAndRedirectsHome | PASS |
| 84 | TS006-R | WEB-R007 | FrontendAuthContractTest.registerJavascript_callsCorrectBackendEndpoint | PASS |
| 85 | TS006-R | WEB-R008 | FrontendAuthContractTest.registerJavascript_validatesPasswordAndRedirectsLogin | PASS |
| 86 | TS006-R | WEB-R006 | FrontendAuthContractTest.registerTemplate_containsInputsUsedByJavascript | PASS |
| 87 | TS006-R | WEB-R003 | FrontendAuthContractTest.loginTemplate_containsInputsUsedByJavascript | PASS |
| 88 | TS006-R | WEB-R004 | FrontendAuthContractTest.loginJavascript_callsCorrectBackendEndpoint | PASS |
| 89 | TS015 | WEBF-001 | FriendNotificationChatContractTest.friendJs_loadsCurrentUserFromUsersMe | PASS |
| 90 | TS015 | WEBF-003 | FriendNotificationChatContractTest.friendJs_opensDirectRoom | PASS |
| 91 | TS015 | WEBF-005 | FriendNotificationChatContractTest.friendJs_sendsTextMessage | PASS |
| 92 | TS015 | WEBF-008 | FriendNotificationChatContractTest.notificationJs_loadsMessageNotifications | PASS |
| 93 | TS015 | WEBF-007 | FriendNotificationChatContractTest.notificationJs_acceptDecline_endpointsMatch | PASS |
| 94 | TS015 | WEBF-006 | FriendNotificationChatContractTest.notificationJs_loadsIncomingFriendRequests | PASS |
| 95 | TS015 | WEBF-002 | FriendNotificationChatContractTest.friendJs_callsFriendsApi | PASS |
| 96 | TS015 | WEBF-004 | FriendNotificationChatContractTest.friendJs_loadsMessageHistory | PASS |
| 97 | JUnit-20261010 | JUNIT-097 | ProfilePersistenceTest.biographyCascadesWithSharedUserIdAndSurvivesReload | PASS |
| 98 | JUnit-20261010 | JUNIT-098 | SchemaExportTest.exportPostgresSchemaFromActualMappings | PASS |
| 99 | TS002-R | LOGIN-R011 | AuthServiceLoginTest.login_blockedAccount_doesNotCheckPassword | PASS |
| 100 | TS002-R | LOGIN-R009 | AuthServiceLoginTest.login_wrongPassword_rejected | PASS |
| 101 | TS002-R | LOGIN-R005 | AuthServiceLoginTest.login_nullPassword_rejected | PASS |
| 102 | TS002-R | LOGIN-R006 | AuthServiceLoginTest.login_blankPassword_rejected | PASS |
| 103 | TS002-R | LOGIN-R002 | AuthServiceLoginTest.login_nullRequest_rejected | PASS |
| 104 | TS002-R | LOGIN-R007 | AuthServiceLoginTest.login_unknownEmail_rejected | PASS |
| 105 | TS002-R | LOGIN-R001 | AuthServiceLoginTest.login_validCredentials_returnsUser | PASS |
| 106 | TS002-R | LOGIN-R008 | AuthServiceLoginTest.login_blockedAccount_rejected | PASS |
| 107 | TS002-R | LOGIN-R010 | AuthServiceLoginTest.login_spacedUppercaseEmail_normalized | PASS |
| 108 | TS002-R | LOGIN-R003 | AuthServiceLoginTest.login_nullEmail_rejected | PASS |
| 109 | TS002-R | LOGIN-R004 | AuthServiceLoginTest.login_blankEmail_rejected | PASS |
| 110 | TS001-R | REG-R010 | AuthServiceRegisterTest.register_password_isEncodedBeforeSave | PASS |
| 111 | JUnit-20261010 | JUNIT-111 | AuthServiceRegisterTest.register_withInterests_savesUserInterests | PASS |
| 112 | TS001-R | REG-R012 | AuthServiceRegisterTest.register_withoutImage_doesNotCallStorage | PASS |
| 113 | TS001-R | REG-R006 | AuthServiceRegisterTest.register_blankPassword_rejected | PASS |
| 114 | TS001-R | REG-R011 | AuthServiceRegisterTest.register_withImage_uploadsAndStoresPublicUrl | PASS |
| 115 | TS001-R | REG-R008 | AuthServiceRegisterTest.register_duplicateEmail_rejected | PASS |
| 116 | TS001-R | REG-R002 | AuthServiceRegisterTest.register_nullRequest_rejected | PASS |
| 117 | TS001-R | REG-R001 | AuthServiceRegisterTest.register_validRequest_savesActiveUser | PASS |
| 118 | JUnit-20261010 | JUNIT-118 | AuthServiceRegisterTest.register_withoutInterests_doesNotSaveUserInterests | PASS |
| 119 | TS001-R | REG-R013 | AuthServiceRegisterTest.register_profileFields_areCopied | PASS |
| 120 | TS001-R | REG-R009 | AuthServiceRegisterTest.register_spacedUppercaseEmail_normalized | PASS |
| 121 | TS001-R | REG-R003 | AuthServiceRegisterTest.register_nullEmail_rejected | PASS |
| 122 | TS001-R | REG-R007 | AuthServiceRegisterTest.register_passwordBelowEightCharacters_rejected | PASS |
| 123 | TS001-R | REG-R004 | AuthServiceRegisterTest.register_blankEmail_rejected | PASS |
| 124 | TS001-R | REG-R005 | AuthServiceRegisterTest.register_nullPassword_rejected | PASS |
| 125 | TS014 | MSG-003 | ChatMessageServiceTest.getMessages_directRoom_reversesAndMarksRead | PASS |
| 126 | TS016 | CALL-002 | ChatMessageServiceTest.relayCallSignal_leave_neverMember_returnsForbidden | PASS |
| 127 | JUnit-20261010 | JUNIT-127 | ChatMessageServiceTest.editingAnotherPersonsMessageIsForbidden | PASS |
| 128 | JUnit-20261010 | JUNIT-128 | ChatMessageServiceTest.rejectsImageAndBlankEdits | PASS |
| 129 | TS014 | MSG-006 | ChatMessageServiceTest.send_direct_persistsBroadcastsAndNotifies | PASS |
| 130 | TS014 | MSG-010 | ChatMessageServiceTest.delete_otherUsersMessage_returnsForbidden | PASS |
| 131 | TS014 | MSG-001 | ChatMessageServiceTest.getMessages_nonMember_returnsForbidden | PASS |
| 132 | TS014 | MSG-004 | ChatMessageServiceTest.getMessages_limitAndBefore_areHandled | PASS |
| 133 | TS014 | MSG-008 | ChatMessageServiceTest.send_messageType_defaultAndInvalid_areHandled | PASS |
| 134 | TS014 | MSG-009 | ChatMessageServiceTest.delete_ownMessage_softDeletes | PASS |
| 135 | JUnit-20261010 | JUNIT-135 | ChatMessageServiceTest.editsOnlyOwnTextAndBroadcastsUpdate | PASS |
| 136 | JUnit-20261010 | JUNIT-136 | ChatMessageServiceTest.relayCallInvite_accept_preservesModeAndOldClientsDefaultToVoice | PASS |
| 137 | JUnit-20261010 | JUNIT-137 | ChatMessageServiceTest.cannotEditWithoutMembership | PASS |
| 138 | TS014 | MSG-005 | ChatMessageServiceTest.unreadCount_usesCorrectRepositoryQuery | PASS |
| 139 | TS014 | MSG-007 | ChatMessageServiceTest.send_group_broadcastsWithoutPersistence | PASS |
| 140 | JUnit-20261010 | JUNIT-140 | ChatMessageServiceTest.relayCallInvite_invalidMode_isRejected | PASS |
| 141 | TS014 | MSG-002 | ChatMessageServiceTest.getMessages_groupRoom_returnsEmpty | PASS |
| 142 | JUnit-20261010 | JUNIT-142 | ChatMessageServiceTest.relayCallInvite_videoMode_reachesReceiver | PASS |
| 143 | TS016 | CALL-001 | ChatMessageServiceTest.relayCallSignal_leave_afterMembershipEnded_stillBroadcasts | PASS |
| 144 | TS013 | ROOM-003 | ChatRoomServiceTest.createGroupRoom_private_encodesPassword | PASS |
| 145 | TS013 | ROOM-002 | ChatRoomServiceTest.createGroupRoom_privateWithoutPassword_returnsBadRequest | PASS |
| 146 | JUnit-20261010 | JUNIT-146 | ChatRoomServiceTest.directRoom_keepsExistingConversationId | PASS |
| 147 | TS013 | ROOM-010 | ChatRoomServiceTest.updateRoom_permissionAndMemberLimit_areEnforced | PASS |
| 148 | JUnit-20261010 | JUNIT-148 | ChatRoomServiceTest.discoverSecondPageDoesNotSkipTwice | PASS |
| 149 | TS013 | ROOM-001 | ChatRoomServiceTest.createGroupRoom_publicRoom_appliesDefaultsAndOwner | PASS |
| 150 | TS013 | ROOM-009 | ChatRoomServiceTest.leaveRoom_nonMember_returnsNotFound | PASS |
| 151 | JUnit-20261010 | JUNIT-151 | ChatRoomServiceTest.discoveryFiltersYearAndSortsName | PASS |
| 152 | TS013 | ROOM-008 | ChatRoomServiceTest.joinRoom_fullRoom_returnsConflict | PASS |
| 153 | JUnit-20261010 | JUNIT-153 | ChatRoomServiceTest.joinRoom_existingMember_resumesWithoutDuplicatingMembership | PASS |
| 154 | TS013 | ROOM-005 | ChatRoomServiceTest.discoverRooms_searchAndInterests_intersectsResults | PASS |
| 155 | JUnit-20261010 | JUNIT-155 | ChatRoomServiceTest.onlyOwnerCanDeleteRoom | PASS |
| 156 | TS013 | ROOM-007 | ChatRoomServiceTest.joinRoom_privateWrongPassword_returnsForbidden | PASS |
| 157 | JUnit-20261010 | JUNIT-157 | ChatRoomServiceTest.directRoom_reopensHistoricalConversationWithoutCreatingRoom | PASS |
| 158 | JUnit-20261010 | JUNIT-158 | ChatRoomServiceTest.rejectsUnknownSortWithoutQuerying | PASS |
| 159 | TS013 | ROOM-011 | ChatRoomServiceTest.directRoom_selfOrNonFriend_isRejected | PASS |
| 160 | TS013 | ROOM-004 | ChatRoomServiceTest.createGroupRoom_invalidInterest_returnsBadRequest | PASS |
| 161 | JUnit-20261010 | JUNIT-161 | ChatRoomServiceTest.deletingRoomClosesMembershipWithoutErasingRecords | PASS |
| 162 | JUnit-20261010 | JUNIT-162 | ChatRoomServiceTest.joinRoom_existingMember_resumesPrivateFullRoomWithoutPassword | PASS |
| 163 | TS009 | FRQ-009 | FriendRequestServiceTest.accept_existingFriendship_doesNotDuplicate | PASS |
| 164 | TS009 | FRQ-008 | FriendRequestServiceTest.accept_validReceiver_createsFriendshipAndPublishesEvent | PASS |
| 165 | TS009 | FRQ-006 | FriendRequestServiceTest.incoming_returnsPendingRequests | PASS |
| 166 | TS009 | FRQ-012 | FriendRequestServiceTest.accept_missingRequest_returnsNotFound | PASS |
| 167 | TS009 | FRQ-007 | FriendRequestServiceTest.outgoing_returnsPendingRequests | PASS |
| 168 | TS009 | FRQ-011 | FriendRequestServiceTest.accept_nonPending_returnsConflict | PASS |
| 169 | TS009 | FRQ-002 | FriendRequestServiceTest.send_toSelf_returnsBadRequest | PASS |
| 170 | TS009 | FRQ-004 | FriendRequestServiceTest.send_pendingRequest_returnsConflict | PASS |
| 171 | TS009 | FRQ-010 | FriendRequestServiceTest.accept_nonReceiver_returnsForbidden | PASS |
| 172 | TS009 | FRQ-013 | FriendRequestServiceTest.decline_validReceiver_marksDeclined | PASS |
| 173 | TS009 | FRQ-003 | FriendRequestServiceTest.send_existingFriendship_returnsConflict | PASS |
| 174 | TS009 | FRQ-001 | FriendRequestServiceTest.send_validUsers_savesPendingAndPublishesEvent | PASS |
| 175 | TS009 | FRQ-005 | FriendRequestServiceTest.send_receiverUnavailable_returnsNotFound | PASS |
| 176 | TS010 | FRI-003 | FriendshipServiceTest.unfriend_existingFriendship_deletesRelation | PASS |
| 177 | TS010 | FRI-002 | FriendshipServiceTest.listFriends_inactiveOrMissingUser_returnsNotFound | PASS |
| 178 | TS010 | FRI-004 | FriendshipServiceTest.unfriend_missingFriendship_returnsNotFound | PASS |
| 179 | TS010 | FRI-001 | FriendshipServiceTest.listFriends_returnsMappedFriends | PASS |
| 180 | JUnit-20261010 | JUNIT-180 | ImageContentStrategyTest.savesRealImageUrl | PASS |
| 181 | JUnit-20261010 | JUNIT-181 | ImageContentStrategyTest.refusesSvgBeforeStorage | PASS |
| 182 | JUnit-20261010 | JUNIT-182 | ImageContentStrategyTest.refusesNonImageBeforeStorage | PASS |
| 183 | TS005-R | INT-R003 | InterestServiceTest.createInterest_duplicateName_returnsConflict | PASS |
| 184 | TS005-R | INT-R002 | InterestServiceTest.createInterest_validName_trimsAndSaves | PASS |
| 185 | TS005-R | INT-R001 | InterestServiceTest.allInterests_returnsActiveInterestsOrderedByRepository | PASS |
| 186 | TS005-R | INT-R004 | InterestServiceTest.searchInterests_trimsSearchText | PASS |
| 187 | TS005-R | INT-R005 | InterestServiceTest.searchInterests_blankText_returnsBadRequest | PASS |
| 188 | TS011 | MAT-004 | MatchingServiceTest.testUserNotFoundThrows | PASS |
| 189 | TS011 | MAT-003 | MatchingServiceTest.testCalculateCompatibility | PASS |
| 190 | TS011 | MAT-001 | MatchingServiceTest.testRecommendFriendsRanked | PASS |
| 191 | TS011 | MAT-002 | MatchingServiceTest.testExcludeExistingFriendsAndPending | PASS |
| 192 | TS012 | NOT-003 | NotificationServiceTest.list_outOfRangePagination_isClamped | PASS |
| 193 | TS012 | NOT-008 | NotificationServiceTest.onFriendRequestAccepted_savesAndPushesNotification | PASS |
| 194 | TS012 | NOT-005 | NotificationServiceTest.markAsRead_callsScopedUpdate | PASS |
| 195 | TS012 | NOT-001 | NotificationServiceTest.list_allNotifications_mapsResponses | PASS |
| 196 | TS012 | NOT-007 | NotificationServiceTest.onFriendRequestSent_savesAndPushesNotification | PASS |
| 197 | TS012 | NOT-009 | NotificationServiceTest.notifyNewMessage_reusesUnreadAndSkipsSender | PASS |
| 198 | TS012 | NOT-002 | NotificationServiceTest.list_unreadOnly_usesUnreadRepositoryQuery | PASS |
| 199 | TS012 | NOT-004 | NotificationServiceTest.unreadCount_returnsRepositoryCount | PASS |
| 200 | TS012 | NOT-006 | NotificationServiceTest.markAllAsRead_callsRepository | PASS |
| 201 | JUnit-20261010 | JUNIT-201 | ProfileGalleryTest.retainedPhotosCanBeReordered | PASS |
| 202 | JUnit-20261010 | JUNIT-202 | ProfileGalleryTest.emptyGalleryClearsAndAllowsAvatarFallback | PASS |
| 203 | JUnit-20261010 | JUNIT-203 | ProfileGalleryTest.realImageIsUploadedAndIncludedInProfileResponse | PASS |
| 204 | JUnit-20261010 | JUNIT-204 | ProfileGalleryTest.disguisedImageIsRejectedBeforeUpload | PASS |
| 205 | JUnit-20261010 | JUNIT-205 | ProfileGalleryTest.moreThanFivePhotosIsRejected | PASS |
| 206 | JUnit-20261010 | JUNIT-206 | ProfileGalleryTest.interestSelectionIsUpdatedWithProfile | PASS |
| 207 | JUnit-20261010 | JUNIT-207 | ProfileGalleryTest.omittedGalleryPreservesExistingPhotos | PASS |
| 208 | JUnit-20261010 | JUNIT-208 | ProfileGalleryTest.anotherUsersPhotoUrlIsRejected | PASS |
| 209 | JUnit-20261010 | JUNIT-209 | ProfileGalleryTest.validAndEmptyPhotoSourcesCannotBeMixed | PASS |
| 210 | JUnit-20261010 | JUNIT-210 | RoomRealtimeServiceTest.mediaAloneDoesNotCreateParticipant | PASS |
| 211 | JUnit-20261010 | JUNIT-211 | RoomRealtimeServiceTest.membershipBroadcastsToRoomAndHome | PASS |
| 212 | JUnit-20261010 | JUNIT-212 | RoomRealtimeServiceTest.lateObserverReceivesExistingParticipantsAndMedia | PASS |
| 213 | JUnit-20261010 | JUNIT-213 | RoomRealtimeServiceTest.explicitLeaveRemovesParticipantAndAdvancesRevision | PASS |
| 214 | JUnit-20261010 | JUNIT-214 | RoomRealtimeServiceTest.disconnectKeepsAnotherTabUntilLastSessionCloses | PASS |
| 215 | JUnit-20261010 | JUNIT-215 | RoomRealtimeServiceTest.leavingMembershipRemovesAllCallSessions | PASS |
| 216 | TS010 | DISC-002 | UserDiscoveryServiceTest.discover_excludesSelfFriendsAndPending | PASS |
| 217 | TS010 | DISC-004 | UserDiscoveryServiceTest.discover_departmentFilter_isTrimmedAndCaseInsensitive | PASS |
| 218 | TS010 | DISC-003 | UserDiscoveryServiceTest.discover_excludesInactiveCandidates | PASS |
| 219 | TS010 | DISC-005 | UserDiscoveryServiceTest.discover_yearFilter_returnsMatchingYear | PASS |
| 220 | TS010 | DISC-001 | UserDiscoveryServiceTest.discover_noMatches_returnsEmpty | PASS |
| 221 | TS008 | UINT-004 | UserInterestServiceTest.replaceInterests_validIds_replacesAndReturnsSortedList | PASS |
| 222 | TS008 | UINT-003 | UserInterestServiceTest.getInterests_missingUser_returnsNotFound | PASS |
| 223 | TS008 | UINT-006 | UserInterestServiceTest.replaceInterests_emptyList_clearsAllInterests | PASS |
| 224 | TS008 | UINT-005 | UserInterestServiceTest.replaceInterests_duplicateIds_savesOnlyOneRelation | PASS |
| 225 | TS008 | UINT-001 | UserInterestServiceTest.getInterests_existingUser_returnsActiveInterestsSortedByName | PASS |
| 226 | TS008 | UINT-007 | UserInterestServiceTest.replaceInterests_missingOrInactiveInterest_returnsBadRequest | PASS |
| 227 | TS008 | UINT-002 | UserInterestServiceTest.getInterests_noAssignedInterests_returnsEmptyList | PASS |
| 228 | TS007 | PRO-003 | UserServiceTest.updateProfile_updatesProvidedFields | PASS |
| 229 | TS007 | PRO-006 | UserServiceTest.updateProfile_withoutImage_doesNotCallStorage | PASS |
| 230 | TS007 | PRO-004 | UserServiceTest.updateProfile_omittedFields_keepOldValues | PASS |
| 231 | TS007 | PRO-002 | UserServiceTest.getProfile_missingUser_returnsNotFound | PASS |
| 232 | TS007 | PRO-007 | UserServiceTest.updateProfile_blankFirstname_clearsFirstname | PASS |
| 233 | TS007 | PRO-005 | UserServiceTest.updateProfile_withImage_uploadsAndStoresUrl | PASS |
| 234 | TS007 | PRO-001 | UserServiceTest.getProfile_existingUser_returnsUser | PASS |
| 235 | TS011 | MAT-008 | InterestMatchingStrategyTest.testCaseInsensitiveMatching | PASS |
| 236 | TS011 | MAT-006 | InterestMatchingStrategyTest.testFullMatch | PASS |
| 237 | TS011 | MAT-010 | InterestMatchingStrategyTest.testStrategyName | PASS |
| 238 | TS011 | MAT-009 | InterestMatchingStrategyTest.testEmptyAndNullScenarios | PASS |
| 239 | TS011 | MAT-007 | InterestMatchingStrategyTest.testZeroMatch | PASS |
| 240 | TS011 | MAT-005 | InterestMatchingStrategyTest.testPartialMatchFromRequirements | PASS |
