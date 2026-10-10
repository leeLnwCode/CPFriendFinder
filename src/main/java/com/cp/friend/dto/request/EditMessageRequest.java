package com.cp.friend.dto.request;
import jakarta.validation.constraints.*;
public record EditMessageRequest(@NotBlank @Size(max=5000) String content) {}
