package com.cp.friend.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

// ตาราง chat_rooms (GROUP = ห้องใน Home, DIRECT = แชทส่วนตัวใน Friend Page)
@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {

    public enum RoomType { GROUP, DIRECT }

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType = RoomType.GROUP;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "target_year")
    private Short targetYear;

    @Column(name = "max_members", nullable = false)
    private Short maxMembers = 10;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @OneToMany(mappedBy = "room", fetch = FetchType.LAZY)
    private java.util.List<RoomMember> members = new java.util.ArrayList<>();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
