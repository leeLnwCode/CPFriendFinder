package com.cp.friend.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
@Entity
@Table(name="user_profiles")
@Getter @Setter @NoArgsConstructor
public class UserProfile {
 @Id private UUID id;
 @OneToOne(fetch=FetchType.LAZY, optional=false) @MapsId @JoinColumn(name="user_id")
 private User user;
 @Column(length=500) private String bio;
 public UserProfile(User user, String bio) { this.user=user; this.bio=bio; }
}
