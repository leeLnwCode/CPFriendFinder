package com.cp.friend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class LoginRequest {

	@NotBlank(message = "email is required")
	@Email(message = "email is invalid")
    private String email;
	@NotBlank(message = "password is required")
    private String password;
    public LoginRequest(){}
}

