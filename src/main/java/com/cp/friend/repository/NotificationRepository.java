package com.cp.friend.repository;

import com.cp.friend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง notifications
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // รายการแจ้งเตือนของ user ใหม่สุดก่อน (แบ่งหน้า) — ดึง actor มาพร้อมกัน
    @Query(value = """
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.actor
            WHERE n.user.id = :userId
            ORDER BY n.createdAt DESC
            """,
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId")
    Page<Notification> findByUserId(@Param("userId") UUID userId, Pageable pageable);

    // เฉพาะที่ยังไม่อ่าน
    @Query(value = """
            SELECT n FROM Notification n
            LEFT JOIN FETCH n.actor
            WHERE n.user.id = :userId AND n.isRead = false
            ORDER BY n.createdAt DESC
            """,
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    Page<Notification> findUnreadByUserId(@Param("userId") UUID userId, Pageable pageable);

    // จำนวนที่ยังไม่อ่าน (badge ที่กระดิ่ง)
    long countByUserIdAndIsReadFalse(UUID userId);

    // ทำเครื่องหมายว่าอ่านแล้ว 1 รายการ (ตรวจว่าเป็นของ user นั้นจริง)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markAsRead(@Param("id") UUID id, @Param("userId") UUID userId);

    // อ่านทั้งหมด
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(@Param("userId") UUID userId);

    // notification "ข้อความใหม่ในห้อง" ที่ยังไม่อ่าน — ใช้รวมหลายข้อความเป็นรายการเดียว (กันสแปม)
    Optional<Notification> findFirstByUserIdAndRoomIdAndTypeAndIsReadFalse(
            UUID userId, UUID roomId, Notification.Type type);
}
