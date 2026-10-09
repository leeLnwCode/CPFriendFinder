package com.cp.friend.dto.response;

import com.cp.friend.model.User;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class UpdateProfileResponse {

    private UUID id;
    private String email;
    private String firstname;
    private String lastname;
    private String bio;
    private LocalDate dateOfBirth;
    private Short year;
    private String department;
    private String image_url;
    private User.Status status;
    private Instant updatedAt;

    public UpdateProfileResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.firstname = user.getFirstname();
        this.lastname = user.getLastname();
        this.bio = user.getBio();
        this.dateOfBirth = user.getDateOfBirth();
        this.year = user.getYear();
        this.department = user.getDepartment();
        this.image_url = user.getImageUrl();
        this.status = user.getStatus();
        this.updatedAt = user.getUpdatedAt();
    }
}
