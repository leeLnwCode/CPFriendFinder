package com.cp.friend.repository;

import com.cp.friend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository สำหรับ User
 *
 * รองรับ:
 * - สร้าง User
 * - แก้ไข User
 * - ลบ User
 * - ค้นหา User
 * - ตรวจสอบ Email
 * - ค้นหา User ที่ ACTIVE
 * - Block / Active
 * - Update last seen
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    // Serialize concurrent direct-room creation on the same canonical participant.
    // Native scalar locking avoids FOR UPDATE on the nullable One-to-One profile join.
    @Query(value="SELECT id FROM users WHERE id = :id FOR UPDATE", nativeQuery=true)
    Optional<UUID> lockDirectChatParticipant(@Param("id") UUID id);


    /**
     * ค้นหา User ด้วย Email
     */
    Optional<User> findByEmail(String email);

    /**
     * ตรวจสอบว่ามี Email นี้แล้วหรือไม่
     */
    boolean existsByEmail(String email);

    /**
     * ค้นหา User ที่ ACTIVE ด้วย Email
     */
    Optional<User> findByEmailAndStatus(
            String email,
            User.Status status
    );

    /**
     * ดึง User ทั้งหมดตาม Status
     */
    List<User> findAllByStatus(User.Status status);

    /**
     * ดึงเฉพาะ User ที่ ACTIVE
     */
    default List<User> findAllActive() {
        return findAllByStatus(User.Status.ACTIVE);
    }

    /**
     * ค้นหาจากชื่อและนามสกุล
     *
     * เช่น keyword = "john"
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.status = :status
              AND (
                    LOWER(u.firstname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.lastname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY u.firstname ASC, u.lastname ASC
            """)
    List<User> searchByName(
            @Param("keyword") String keyword,
            @Param("status") User.Status status
    );

    /**
     * ค้นหา User ที่ ACTIVE จากชื่อ
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.status = com.cp.friend.model.User.Status.ACTIVE
              AND (
                    LOWER(u.firstname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.lastname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            ORDER BY u.firstname ASC, u.lastname ASC
            """)
    List<User> searchActiveByName(
            @Param("keyword") String keyword
    );

    // =========================================================
    // STATUS
    // =========================================================

    /**
     * เปลี่ยน Status ของ User
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.status = :status
            WHERE u.id = :id
            """)
    int updateStatus(
            @Param("id") UUID id,
            @Param("status") User.Status status
    );

    /**
     * Block User
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.status = com.cp.friend.model.User.Status.BLOCKED
            WHERE u.id = :id
            """)
    int blockUser(@Param("id") UUID id);

    /**
     * Activate User
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.status = com.cp.friend.model.User.Status.ACTIVE
            WHERE u.id = :id
            """)
    int activateUser(@Param("id") UUID id);

    // =========================================================
    // LAST SEEN
    // =========================================================

    /**
     * อัปเดตเวลาที่ User ออนไลน์ล่าสุด
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u
            SET u.lastSeenAt = :at
            WHERE u.id = :id
            """)
    int updateLastSeenAt(
            @Param("id") UUID id,
            @Param("at") Instant at
    );

    // =========================================================
    // PROFILE
    // =========================================================

    /**
     * ค้นหาจาก Department
     */
    List<User> findAllByDepartment(String department);

    /**
     * ค้นหา User ที่ ACTIVE ตาม Department
     */
    List<User> findAllByDepartmentAndStatus(
            String department,
            User.Status status
    );

    /**
     * ค้นหาจากปี
     */
    List<User> findAllByYear(Short year);

    /**
     * ค้นหา User ที่ ACTIVE ตามปี
     */
    List<User> findAllByYearAndStatus(
            Short year,
            User.Status status
    );

    /**
     * ค้นหา User ตาม Department และ Year
     */
    List<User> findAllByDepartmentAndYearAndStatus(
            String department,
            Short year,
            User.Status status
    );

    // =========================================================
    // DELETE
    // =========================================================

    /**
     * JpaRepository มี deleteById() ให้แล้ว
     *
     * ตัวอย่าง:
     *
     * userRepository.deleteById(userId);
     */

    /**
     * ลบ User โดย Email
     */
    void deleteByEmail(String email);
}

