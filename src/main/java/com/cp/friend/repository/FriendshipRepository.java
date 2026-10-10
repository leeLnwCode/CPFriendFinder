package com.cp.friend.repository;

import com.cp.friend.model.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง friendships
// เก็บแถวเดียวต่อคู่ ดังนั้นทุกคิวรีต้องดูทั้งสองทิศทาง (user_id หรือ friend_id)
public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    // รายชื่อเพื่อนทั้งหมดของ me (ไม่ว่า me จะอยู่ฝั่ง user หรือ friend)
    @Query("""
            SELECT f FROM Friendship f
            JOIN FETCH f.user
            JOIN FETCH f.friend
            WHERE f.user.id = :me OR f.friend.id = :me
            ORDER BY f.createdAt DESC
            """)
    List<Friendship> findAllByMember(@Param("me") UUID me);

    // หาความสัมพันธ์ระหว่าง 2 คน ไม่สนทิศทาง
    @Query("""
            SELECT f FROM Friendship f
            WHERE (f.user.id = :a AND f.friend.id = :b)
               OR (f.user.id = :b AND f.friend.id = :a)
            """)
    Optional<Friendship> findBetween(@Param("a") UUID a, @Param("b") UUID b);

    // เป็นเพื่อนกันแล้วหรือยัง
    @Query("""
            SELECT (COUNT(f) > 0) FROM Friendship f
            WHERE (f.user.id = :a AND f.friend.id = :b)
               OR (f.user.id = :b AND f.friend.id = :a)
            """)
    boolean existsBetween(@Param("a") UUID a, @Param("b") UUID b);

    // ลบเพื่อน (unfriend) ทั้งคู่ในคำสั่งเดียว
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            DELETE FROM Friendship f
            WHERE (f.user.id = :a AND f.friend.id = :b)
               OR (f.user.id = :b AND f.friend.id = :a)
            """)
    int deleteBetween(@Param("a") UUID a, @Param("b") UUID b);

    @Query("SELECT COUNT(f) FROM Friendship f WHERE f.user.id = :me OR f.friend.id = :me")
    long countByMember(@Param("me") UUID me);
}
