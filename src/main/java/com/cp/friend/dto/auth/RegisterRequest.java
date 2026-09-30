package com.cp.friend.dto.auth;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class RegisterRequest {

    public RegisterRequest(){}

    private String email;

    private String password;

    private String firstname;

    private String lastname;

    private String imageBase64;

    private LocalDate dateOfBirth;

    private Short year;

    private String department;
}
