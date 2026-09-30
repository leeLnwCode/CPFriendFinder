package com.cp.friend.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// ตาราง room_interests (N:M ระหว่าง ChatRoom กับ Interest, PK รวม 2 คอลัมน์)
@Entity
@Table(name = "room_interests")
@IdClass(RoomInterest.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class RoomInterest {

    @Id
    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Id
    @Column(name = "interest_id", nullable = false)
    private UUID interestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", insertable = false, updatable = false)
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", insertable = false, updatable = false)
    private Interest interest;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements java.io.Serializable {
        private UUID roomId;
        private UUID interestId;
    }
}
