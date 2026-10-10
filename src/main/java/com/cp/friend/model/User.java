
package com.cp.friend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    public enum Status {
        ACTIVE,
        BLOCKED
    }

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "image_url")
    private String imageUrl = "avatar.png";

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "lastname")
    private String lastname;

    @Column(length = 500)
    private String bio;

    @OneToOne(mappedBy="user", cascade=CascadeType.ALL, orphanRemoval=true, fetch=FetchType.EAGER)
    private UserProfile profile;

    @ElementCollection
    @CollectionTable(name="user_gallery_photos", joinColumns=@JoinColumn(name="user_id"))
    @OrderColumn(name="photo_order")
    @Column(name="image_url", length=2048, nullable=false)
    @org.hibernate.annotations.BatchSize(size=50)
    private java.util.List<String> galleryPhotos = new java.util.ArrayList<>();

    public java.util.List<String> getGalleryPhotos() { return galleryPhotos; }

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private Short year;

    private String department;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // JPA requires a no-args constructor
    public User() {
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getBio() {
        return profile == null ? bio : profile.getBio();
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Short getYear() {
        return year;
    }

    public String getDepartment() {
        return department;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setBio(String bio) {
        this.bio = bio;
        if (profile == null) profile = new UserProfile(this,bio);
        else profile.setBio(bio);
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public void setYear(Short year) {
        this.year = year;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
