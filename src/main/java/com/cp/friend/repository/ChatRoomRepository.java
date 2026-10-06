package com.cp.friend.repository;

import com.cp.friend.model.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง chat_rooms
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    // ห้อง GROUP ทั้งหมดที่แสดงใน Home (แบ่งหน้า)
    Page<ChatRoom> findByRoomType(ChatRoom.RoomType roomType, Pageable pageable);

    // ห้อง GROUP ที่ค้นหาด้วยชื่อ (ไม่สนตัวพิมพ์เล็ก/ใหญ่)
    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.roomType = com.cp.friend.model.ChatRoom.RoomType.GROUP
              AND LOWER(r.roomName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY r.createdAt DESC
            """)
    List<ChatRoom> searchGroupRoomsByName(@Param("keyword") String keyword);

    // ห้อง GROUP ที่มี interest ตรงกับที่ระบุ (ใช้ filter หน้า Home)
    @Query("""
            SELECT DISTINCT r FROM ChatRoom r
            JOIN RoomInterest ri ON ri.roomId = r.id
            WHERE r.roomType = com.cp.friend.model.ChatRoom.RoomType.GROUP
              AND ri.interestId IN :interestIds
            ORDER BY r.createdAt DESC
            """)
    List<ChatRoom> findGroupRoomsByInterestIds(@Param("interestIds") Collection<UUID> interestIds);

    // ห้องที่ user คนนี้สร้าง
    List<ChatRoom> findByCreatedByIdOrderByCreatedAtDesc(UUID createdById);

    // ห้องที่ user เป็นสมาชิกอยู่ (ยังไม่ออกจากห้อง)
    @Query("""
            SELECT r FROM ChatRoom r
            JOIN RoomMember m ON m.room = r
            WHERE m.user.id = :userId AND m.leftAt IS NULL
            ORDER BY r.updatedAt DESC
            """)
    List<ChatRoom> findJoinedRooms(@Param("userId") UUID userId);

    // หาห้อง DIRECT ที่มีสมาชิกเป็น 2 คนนี้พอดี (ใช้ก่อนสร้างแชทส่วนตัวใหม่ กันสร้างซ้ำ)
    @Query("""
            SELECT r FROM ChatRoom r
            WHERE r.roomType = com.cp.friend.model.ChatRoom.RoomType.DIRECT
              AND EXISTS (SELECT 1 FROM RoomMember m1 WHERE m1.room = r AND m1.user.id = :a)
              AND EXISTS (SELECT 1 FROM RoomMember m2 WHERE m2.room = r AND m2.user.id = :b)
            """)
    Optional<ChatRoom> findDirectRoomBetween(@Param("a") UUID a, @Param("b") UUID b);
}
