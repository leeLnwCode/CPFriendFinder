package com.cp.friend.dto.auth;

import com.cp.friend.model.User;

import java.util.UUID;

public class LoginResponse {

    private UUID id;
    private String email;
    private String firstname;
    private String lastname;
    private User.Status status;

    public LoginResponse(
            UUID id,
            String email,
            String firstname,
            String lastname,
            User.Status status
    ) {
        this.id = id;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public User.Status getStatus() {
        return status;
    }
}

