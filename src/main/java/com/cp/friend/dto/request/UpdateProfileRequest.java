package com.cp.friend.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// ทุก field เป็น optional — ส่งมาแค่ field ไหนอัปเดตเฉพาะ field นั้น
// ส่ง "" มา = ล้างค่า field นั้น
@Getter
@Setter
public class UpdateProfileRequest {

    @Size(max = 255, message = "firstname must be at most 255 characters")
    private String firstname;

    @Size(max = 255, message = "lastname must be at most 255 characters")
    private String lastname;

    @Size(max = 500, message = "bio must be at most 500 characters")
    private String bio;

    @Past(message = "dateOfBirth must be in the past")
    private LocalDate dateOfBirth;

    @Positive(message = "year must be a positive number")
    private Short year;

    @Size(max = 255, message = "department must be at most 255 characters")
    private String department;

    @Pattern(
            regexp = "(?i)^(?:data:image/(?:png|jpe?g|gif|webp);base64,)?(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$",
            message = "imageBase64 must be valid base64 image data"
    )
    private String imageBase64;
}
