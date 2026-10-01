package com.cp.friend.dto.response;

import com.cp.friend.model.User;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter 
public class RegisterResponse {

    private UUID id;
    private String email;
    private String firstname;
    private String lastname;
    private User.Status status;
    private String image_url;

    public RegisterResponse(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.firstname = user.getFirstname();
        this.lastname = user.getLastname();
        this.status = user.getStatus();
        this.image_url = user.getImageUrl();
    }
}

