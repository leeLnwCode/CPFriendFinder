package com.cp.friend.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

// ตาราง user_interests (N:M ระหว่าง User กับ Interest, PK รวม 2 คอลัมน์)
@Entity
@Table(name = "user_interests")
@IdClass(UserInterest.Pk.class)
@Getter
@Setter
@NoArgsConstructor
public class UserInterest {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "interest_id", nullable = false)
    private UUID interestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interest_id", insertable = false, updatable = false)
    private Interest interest;

    @Getter
    @Setter
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Pk implements java.io.Serializable {
        private UUID userId;
        private UUID interestId;
    }
}
