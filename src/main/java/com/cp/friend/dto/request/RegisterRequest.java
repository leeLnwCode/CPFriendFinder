package com.cp.friend.dto.request;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class RegisterRequest {

    public RegisterRequest(){}

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

    @Size(max = 255, message = "firstname must be at most 255 characters")
    private String firstname;

    @Size(max = 255, message = "lastname must be at most 255 characters")
    private String lastname;

    @Pattern(
            regexp = "(?i)^(?:data:image/(?:png|jpe?g|gif|webp);base64,)?(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$",
            message = "imageBase64 must be valid base64 image data"
    )
    private String imageBase64;

    @Past(message = "dateOfBirth must be in the past")
    private LocalDate dateOfBirth;

    @Positive(message = "year must be a positive number")
    private Short year;

    @Size(max = 255, message = "department must be at most 255 characters")
    private String department;

    private List<String> interests;
}
