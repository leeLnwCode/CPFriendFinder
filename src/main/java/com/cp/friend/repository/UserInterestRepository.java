package com.cp.friend.repository;

import com.cp.friend.model.UserInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

// Repository ของตาราง user_interests (PK รวม: userId + interestId)
public interface UserInterestRepository extends JpaRepository<UserInterest, UserInterest.Pk> {

    List<UserInterest> findByUserId(UUID userId);

    // ดึงพร้อม Interest ในคิวรีเดียว กัน N+1
    @Query("SELECT ui FROM UserInterest ui JOIN FETCH ui.interest WHERE ui.userId = :userId")
    List<UserInterest> findByUserIdWithInterest(@Param("userId") UUID userId);

        @Query("""
            SELECT ui FROM UserInterest ui
            JOIN FETCH ui.interest interest
            WHERE ui.userId = :userId AND interest.isActive = true
            ORDER BY interest.name ASC
            """)
        List<UserInterest> findActiveByUserIdWithInterest(@Param("userId") UUID userId);

    @Query("SELECT ui FROM UserInterest ui JOIN FETCH ui.interest WHERE ui.userId IN :userIds")
    List<UserInterest> findByUserIdsWithInterest(@Param("userIds") List<UUID> userIds);

    boolean existsByUserIdAndInterestId(UUID userId, UUID interestId);

    // ล้าง interest ทั้งหมดของ user (ใช้ตอนแก้ไขโปรไฟล์แล้วบันทึกใหม่)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM UserInterest ui WHERE ui.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);

    // นับจำนวน interest ที่ตรงกันระหว่าง user เรา กับ user อื่น ๆ (ใช้จัดอันดับคนที่คล้ายกัน)
    @Query("""
            SELECT other.userId AS userId, COUNT(other.interestId) AS commonCount
            FROM UserInterest mine
            JOIN UserInterest other ON other.interestId = mine.interestId
                        JOIN Interest sharedInterest ON sharedInterest.id = mine.interestId
                        WHERE mine.userId = :userId
                            AND other.userId <> :userId
                            AND sharedInterest.isActive = true
            GROUP BY other.userId
            ORDER BY COUNT(other.interestId) DESC, other.userId ASC
            """)
    List<CommonInterestCount> findUsersWithCommonInterests(@Param("userId") UUID userId);

    // projection สำหรับผลลัพธ์ด้านบน
    interface CommonInterestCount {
        UUID getUserId();

        Long getCommonCount();
    }
}
