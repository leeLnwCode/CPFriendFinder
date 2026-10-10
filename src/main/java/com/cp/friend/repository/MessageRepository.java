package com.cp.friend.repository;

import com.cp.friend.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง messages (soft delete: ข้อความที่ลบแล้วมี deleted_at != null)
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // ข้อความล่าสุดของห้อง (ยังไม่ถูกลบ) ใหม่สุดก่อน — ส่ง PageRequest.of(0, n) เพื่อจำกัดจำนวน
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            WHERE m.room.id = :roomId AND m.deletedAt IS NULL
            ORDER BY m.createdAt DESC
            """)
    List<Message> findLatestByRoomId(@Param("roomId") UUID roomId, Pageable pageable);

    // โหลดข้อความเก่ากว่าเวลาที่กำหนด (infinite scroll ย้อนหลัง)
    @Query("""
            SELECT m FROM Message m
            JOIN FETCH m.sender
            WHERE m.room.id = :roomId AND m.deletedAt IS NULL AND m.createdAt < :before
            ORDER BY m.createdAt DESC
            """)
    List<Message> findByRoomIdBefore(@Param("roomId") UUID roomId,
                                     @Param("before") Instant before,
                                     Pageable pageable);

    // ข้อความล่าสุด 1 ข้อความ (ใช้แสดง preview ในรายการห้อง)
    Optional<Message> findFirstByRoomIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID roomId);

    // Soft delete: ลบได้เฉพาะเจ้าของข้อความ (ตรวจ sender ใน where) คืนจำนวนแถวที่แก้
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Message m SET m.deletedAt = :at, m.updatedAt = :at
            WHERE m.id = :id AND m.sender.id = :senderId AND m.deletedAt IS NULL
            """)
    int softDelete(@Param("id") UUID id, @Param("senderId") UUID senderId, @Param("at") Instant at);

    long countByRoomIdAndDeletedAtIsNull(UUID roomId);

    // นับ unread ของ user ในห้อง: ข้อความของคนอื่น ยังไม่ลบ
    long countByRoomIdAndDeletedAtIsNullAndSenderIdNot(UUID roomId, UUID senderId);

    // นับ unread เมื่อรู้เวลาอ่านล่าสุด (จาก room_members.last_read_at)
    long countByRoomIdAndDeletedAtIsNullAndSenderIdNotAndCreatedAtAfter(
            UUID roomId, UUID senderId, Instant after);
}
