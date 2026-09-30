# รายงานสรุปผลการทดสอบ CPFriendFinder

**วันที่รัน:** 30 กันยายน 2569 เวลา 14:14 น. (+07)  
**ผู้ทดสอบ:** จิรัชญา เป้าจันทึก  
**สภาพแวดล้อม:** Windows 11, JDK 21.0.11, Maven Wrapper 3.9.16  
**คำสั่ง:** `./mvnw -Djava.version=21 clean test`

## ผลการทดสอบ

| ชุดทดสอบ | รัน | ผ่าน | ไม่ผ่าน | Error | ข้าม |
|---|---:|---:|---:|---:|---:|
| `FriendApplicationTests` | 1 | 1 | 0 | 0 | 0 |
| `AuthServiceRegisterTest` | 12 | 12 | 0 | 0 | 0 |
| `AuthServiceLoginTest` | 11 | 11 | 0 | 0 | 0 |
| `AuthServiceTest` | 1 | 1 | 0 | 0 | 0 |
| **รวม** | **25** | **25** | **0** | **0** | **0** |

**ผลจาก Maven:** `BUILD SUCCESS`  
**ระยะเวลา:** 1 นาที 34 วินาที

Test Case ใน Excel ครอบคลุม `AuthServiceRegisterTest` 12 กรณี และ `AuthServiceLoginTest` 11 กรณี รวม 23 กรณี ส่วนผลรวมจาก Maven มี `FriendApplicationTests` และ `AuthServiceTest` เพิ่มอีกอย่างละ 1 รายการ จึงเป็น 25 รายการ

**ขอบเขต:** ผลรายงานนี้ครอบคลุมการโหลด Spring Context และการทดสอบ AuthService ด้วย JUnit 5/Mockito ไม่รวมการทดสอบ Controller, API, ฐานข้อมูลจริง หรือ UAT
