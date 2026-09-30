package com.cp.friend.repository;

import com.cp.friend.model.RandomChatSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง random_chat_sessions
// สถานะ: WAITING -> MATCHED -> ENDED / CANCELLED
public interface RandomChatSessionRepository extends JpaRepository<RandomChatSession, UUID> {

    // หาห้องที่กำลังรอคู่ (ไม่ใช่ของตัวเอง) เก่าสุดก่อน
    // ล็อกแถวแบบ pessimistic กัน 2 คนแย่งจับคู่กับห้องเดียวกัน — ต้องเรียกใน @Transactional
    // ใช้ Pageable (PageRequest.of(0, 1)) จำกัดผลลัพธ์เป็น 1 แถว
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s FROM RandomChatSession s
            WHERE s.status = com.cp.friend.model.RandomChatSession.Status.WAITING
              AND s.user1.id <> :userId
            ORDER BY s.startedAt ASC
            """)
    List<RandomChatSession> findWaitingForMatch(@Param("userId") UUID userId, Pageable pageable);

    // session ที่ user ยัง active อยู่ (WAITING หรือ MATCHED) ไม่ว่าจะเป็น user1 หรือ user2
    @Query("""
            SELECT s FROM RandomChatSession s
            WHERE s.status IN (com.cp.friend.model.RandomChatSession.Status.WAITING,
                               com.cp.friend.model.RandomChatSession.Status.MATCHED)
              AND (s.user1.id = :userId OR s.user2.id = :userId)
            """)
    Optional<RandomChatSession> findActiveByUserId(@Param("userId") UUID userId);

    @Query("""
            SELECT (COUNT(s) > 0) FROM RandomChatSession s
            WHERE s.status IN (com.cp.friend.model.RandomChatSession.Status.WAITING,
                               com.cp.friend.model.RandomChatSession.Status.MATCHED)
              AND (s.user1.id = :userId OR s.user2.id = :userId)
            """)
    boolean hasActiveSession(@Param("userId") UUID userId);

    // ประวัติ session ของ user (ที่จบแล้ว) ใหม่สุดก่อน
    @Query("""
            SELECT s FROM RandomChatSession s
            WHERE (s.user1.id = :userId OR s.user2.id = :userId)
              AND s.status = com.cp.friend.model.RandomChatSession.Status.ENDED
            ORDER BY s.endedAt DESC
            """)
    List<RandomChatSession> findEndedByUserId(@Param("userId") UUID userId);

    long countByStatus(RandomChatSession.Status status);
}
