package com.cp.friend.repository;

import com.cp.friend.model.RoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง room_members
// "สมาชิกปัจจุบัน" = แถวที่ left_at IS NULL
public interface RoomMemberRepository extends JpaRepository<RoomMember, UUID> {

    // สมาชิกปัจจุบันของห้อง พร้อม user (กัน N+1)
    @Query("""
            SELECT m FROM RoomMember m
            JOIN FETCH m.user
            WHERE m.room.id = :roomId AND m.leftAt IS NULL
            ORDER BY m.joinedAt ASC
            """)
    List<RoomMember> findActiveMembers(@Param("roomId") UUID roomId);

    // แถวสมาชิกปัจจุบันของ user ในห้องนี้ (ถ้ามี)
    @Query("""
            SELECT m FROM RoomMember m
            WHERE m.room.id = :roomId AND m.user.id = :userId AND m.leftAt IS NULL
            """)
    Optional<RoomMember> findActiveMember(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    // แถวล่าสุดของ user ในห้อง (รวมที่ออกไปแล้ว) ใช้ตอน re-join
    Optional<RoomMember> findFirstByRoomIdAndUserIdOrderByJoinedAtDesc(UUID roomId, UUID userId);

    @Query("""
            SELECT (COUNT(m) > 0) FROM RoomMember m
            WHERE m.room.id = :roomId AND m.user.id = :userId AND m.leftAt IS NULL
            """)
    boolean isActiveMember(@Param("roomId") UUID roomId, @Param("userId") UUID userId);

    // จำนวนสมาชิกปัจจุบัน (เทียบกับ ChatRoom.maxMembers ตอนเข้าห้อง)
    @Query("SELECT COUNT(m) FROM RoomMember m WHERE m.room.id = :roomId AND m.leftAt IS NULL")
    long countActiveMembers(@Param("roomId") UUID roomId);

    // ตรวจสิทธิ์ตาม role (เช่น OWNER / MODERATOR)
    @Query("""
            SELECT (COUNT(m) > 0) FROM RoomMember m
            WHERE m.room.id = :roomId AND m.user.id = :userId
              AND m.role IN :roles AND m.leftAt IS NULL
            """)
    boolean hasAnyRole(@Param("roomId") UUID roomId,
                       @Param("userId") UUID userId,
                       @Param("roles") Collection<RoomMember.Role> roles);
}
