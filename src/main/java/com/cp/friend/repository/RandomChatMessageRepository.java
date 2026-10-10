package com.cp.friend.repository;

import com.cp.friend.model.RandomChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// Repository ของตาราง random_chat_messages (soft delete ผ่าน deleted_at)
public interface RandomChatMessageRepository extends JpaRepository<RandomChatMessage, UUID> {

    // ข้อความใน session เรียงเก่า -> ใหม่ (ยังไม่ถูกลบ)
    @Query("""
            SELECT m FROM RandomChatMessage m
            JOIN FETCH m.sender
            WHERE m.session.id = :sessionId AND m.deletedAt IS NULL
            ORDER BY m.createdAt ASC
            """)
    List<RandomChatMessage> findBySessionId(@Param("sessionId") UUID sessionId);

    // ข้อความล่าสุด (ใหม่สุดก่อน) ส่ง PageRequest.of(0, n) เพื่อจำกัดจำนวน
    @Query("""
            SELECT m FROM RandomChatMessage m
            JOIN FETCH m.sender
            WHERE m.session.id = :sessionId AND m.deletedAt IS NULL
            ORDER BY m.createdAt DESC
            """)
    List<RandomChatMessage> findLatestBySessionId(@Param("sessionId") UUID sessionId, Pageable pageable);

    // Soft delete: ลบได้เฉพาะเจ้าของข้อความ คืนจำนวนแถวที่แก้
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RandomChatMessage m SET m.deletedAt = :at, m.updatedAt = :at
            WHERE m.id = :id AND m.sender.id = :senderId AND m.deletedAt IS NULL
            """)
    int softDelete(@Param("id") UUID id, @Param("senderId") UUID senderId, @Param("at") Instant at);

    long countBySessionIdAndDeletedAtIsNull(UUID sessionId);
}
