package com.cp.friend.repository;

import com.cp.friend.model.FriendRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Repository ของตาราง friend_requests
public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    // คำขอที่ส่งถึงเรา (หน้า Friend Request) เรียงใหม่สุดก่อน
    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.sender
            WHERE fr.receiver.id = :receiverId AND fr.status = :status
            ORDER BY fr.createdAt DESC
            """)
    List<FriendRequest> findByReceiverIdAndStatus(@Param("receiverId") UUID receiverId,
                                                  @Param("status") FriendRequest.Status status);

    // คำขอที่เราส่งออกไป
    @Query("""
            SELECT fr FROM FriendRequest fr
            JOIN FETCH fr.receiver
            WHERE fr.sender.id = :senderId AND fr.status = :status
            ORDER BY fr.createdAt DESC
            """)
    List<FriendRequest> findBySenderIdAndStatus(@Param("senderId") UUID senderId,
                                                @Param("status") FriendRequest.Status status);

    // เช็คว่ามีคำขอ PENDING ระหว่าง 2 คนอยู่แล้วหรือยัง (ไม่สนทิศทาง) กันส่งซ้ำ
    @Query("""
            SELECT (COUNT(fr) > 0) FROM FriendRequest fr
            WHERE fr.status = com.cp.friend.model.FriendRequest.Status.PENDING
              AND ((fr.sender.id = :a AND fr.receiver.id = :b)
                OR (fr.sender.id = :b AND fr.receiver.id = :a))
            """)
    boolean existsPendingBetween(@Param("a") UUID a, @Param("b") UUID b);

    // หาคำขอ PENDING ตามทิศทางที่กำหนด (ใช้ตอน accept / decline)
    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(UUID senderId,
                                                                 UUID receiverId,
                                                                 FriendRequest.Status status);

    long countByReceiverIdAndStatus(UUID receiverId, FriendRequest.Status status);
}
