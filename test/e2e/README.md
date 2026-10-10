# Robot Framework Browser E2E

[หน้ารวมงานทดสอบ](../README.md) · [รายงานล่าสุด](../reports/test-report.md) · [Excel](../test-cases/CPFriendFinder_TestCases.xlsx)

## ชุดทดสอบ

| ไฟล์ | เคส | สิ่งที่ตรวจ |
|---|---|---|
| [login_smoke.robot](robot/tests/login_smoke.robot) | E2E-001–003 | หน้า login, แสดง/ซ่อนรหัสผ่าน และลิงก์สมัคร |
| [auth_flow.robot](robot/tests/auth_flow.robot) | E2E-004 | สมัครสมาชิก เติมข้อมูลหน้า login และเข้าสู่ระบบ |
| [register_interest.robot](robot/tests/register_interest.robot) | E2E-005 | บันทึกความสนใจที่เลือกตอนสมัคร |
| [authenticated_regression.robot](robot/tests/authenticated_regression.robot) | E2E-006–015 | home, เพื่อน, แจ้งเตือน, สุ่มคุย, โปรไฟล์/bio/ความสนใจ, responsive, logout และ API 401 |

ใช้ [common.resource](robot/resources/common.resource) ร่วมกัน ผลเว็บจริงรอบ `full-20261010-182412` วันที่ 10 ตุลาคม 2026 เวลา 18:24–18:25 น. ผ่าน **15/15 เคส** ใช้เวลา **62.750 วินาที**

## เตรียมเครื่องครั้งแรก

ต้องมี Python และ Node.js รันจาก root โปรเจกต์ใน Git Bash:

```bash
py -3 -m venv .venv-robot
./.venv-robot/Scripts/python.exe -m pip install -r test/e2e/requirements.txt
./.venv-robot/Scripts/python.exe -m Browser.entry init chromium
```

เวอร์ชันเครื่องมืออยู่ใน [requirements.txt](requirements.txt)

## ตรวจโครงสร้างและ keyword

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
./.venv-robot/Scripts/python.exe -m robot --dryrun   --outputdir "test/e2e/robot/results/dryrun-$RUN_ID" test/e2e/robot/tests
```

dry-run เป็นการตรวจชุดทดสอบ แยกจากผลเว็บจริง

## รัน Login smoke

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
./.venv-robot/Scripts/python.exe -m robot   --variable BASE_URL:https://cpfriendfinder-final-production.up.railway.app   --outputdir "test/e2e/robot/results/smoke-$RUN_ID"   test/e2e/robot/tests/login_smoke.robot
```

## รันครบ 15 เคส

```bash
RUN_ID="$(date +%Y%m%d-%H%M%S)"
./.venv-robot/Scripts/python.exe -m robot   --variable BASE_URL:https://cpfriendfinder-final-production.up.railway.app   --outputdir "test/e2e/robot/results/full-$RUN_ID" test/e2e/robot/tests
```

ชุดเต็มสร้างบัญชีทดสอบใหม่ 3 บัญชีต่อรอบ ใช้ BASE_URL ที่ไม่มี `/login` ต่อท้าย เพิ่ม `--variable HEADLESS:True` หากต้องการซ่อนหน้าต่างเบราว์เซอร์

## อ่านและบันทึกผล

เปิด `report.html` ในโฟลเดอร์ผลรอบนั้นเพื่อดูภาพรวม และ `log.html` เพื่อดูขั้นตอนรายเคส ใช้ `output.xml` สำหรับดึงผลลง Excel สร้าง outputdir ใหม่ทุกครั้งเพื่อเก็บประวัติ เก็บ raw log ไว้ในเครื่องเพราะมีข้อมูลบัญชีทดสอบ ส่วนผลที่อ่านบน GitHub อยู่ใน [รายงานรวม](../reports/test-report.md) และ [JSON](../reports/test-results.json)

E2E-015 ตรวจ API 401 หลังลบ cookie ส่วน session หมดอายุจริงอยู่ใน MAN-018 คอลสองเครื่อง/ต่างเครือข่าย แชร์หน้าจอ CRUD ห้อง/ข้อความและรูปภาพบันทึกอยู่ในส่วน [Manual รายเคส](../reports/test-report.md#ผล-manual-รายเคส)
