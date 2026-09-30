package com.cp.friend.repository;

import com.cp.friend.model.RoomInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

// Repository ของตาราง room_interests (PK รวม: roomId + interestId)
public interface RoomInterestRepository extends JpaRepository<RoomInterest, RoomInterest.Pk> {

    List<RoomInterest> findByRoomId(UUID roomId);

    // ดึงพร้อม Interest ในคิวรีเดียว กัน N+1
    @Query("SELECT ri FROM RoomInterest ri JOIN FETCH ri.interest WHERE ri.roomId = :roomId")
    List<RoomInterest> findByRoomIdWithInterest(@Param("roomId") UUID roomId);

    // ดึง interest ของหลายห้องทีเดียว (ใช้ประกอบหน้า Home)
    @Query("SELECT ri FROM RoomInterest ri JOIN FETCH ri.interest WHERE ri.roomId IN :roomIds")
    List<RoomInterest> findByRoomIdInWithInterest(@Param("roomIds") Collection<UUID> roomIds);

    // ล้าง interest ทั้งหมดของห้อง (ใช้ตอนแก้ไขห้องแล้วบันทึกใหม่)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM RoomInterest ri WHERE ri.roomId = :roomId")
    int deleteAllByRoomId(@Param("roomId") UUID roomId);
}
