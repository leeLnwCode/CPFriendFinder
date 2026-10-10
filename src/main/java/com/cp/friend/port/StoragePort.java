package com.cp.friend.port;

// Adapter Pattern — "พอร์ต" ที่ service layer พึ่ง (Dependency Inversion)
// service ไม่รู้จัก AWS S3 SDK / S3Config เลย — รู้จักแค่สัญญานี้
// วันหนึ่งเปลี่ยน storage เป็น GCS/local disk = เขียน Adapter ใหม่ ไม่แก้ service
public interface StoragePort {

    // อัปโหลด base64 (รองรับ data:image/...;base64, prefix) — คืน object key
    String uploadBase64(String base64);

    // แปลง object key เป็น URL สาธารณะสำหรับเปิดดู
    String publicUrl(String key);
}
