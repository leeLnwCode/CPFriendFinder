package com.cp.friend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

// ตาราง friend_requests
@Entity
@Table(name = "friend_requests")
@Getter
@Setter
@NoArgsConstructor
public class FriendRequest {

    // ให้ตรงกับ CHECK constraint ใน DB — UI/diagram ใช้คำว่า Decline ไม่ใช่ Reject
    public enum Status { PENDING, ACCEPTED, DECLINED }

    @Id
    @UuidGenerator
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "responded_at")
    private Instant respondedAt;
}
