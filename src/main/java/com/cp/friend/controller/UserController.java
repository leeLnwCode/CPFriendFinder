package com.cp.friend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.UpdateProfileRequest;
import com.cp.friend.dto.response.UpdateProfileResponse;
import com.cp.friend.model.User;
import com.cp.friend.service.UserService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController extends SessionController {

    private final UserService userService;

    // Get own profile
    @GetMapping("/me")
    public ResponseEntity<UpdateProfileResponse> getMe(HttpSession session) {
        User user = userService.getProfile(currentUserId(session));
        UpdateProfileResponse response = new UpdateProfileResponse(user);
        return ResponseEntity.ok(response);
    }

    // Update own profile
    @PostMapping("/me")
    public ResponseEntity<UpdateProfileResponse> updateMe(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpSession session
    ) {
        User user = userService.updateProfile(currentUserId(session), request);
        UpdateProfileResponse response = new UpdateProfileResponse(user);
        return ResponseEntity.ok(response);
    }
}
