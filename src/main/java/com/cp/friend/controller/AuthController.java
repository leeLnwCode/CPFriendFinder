package com.cp.friend.controller;

import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.LoginResponse;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.dto.auth.RegisterResponse;
import com.cp.friend.model.User;
import com.cp.friend.service.AuthService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @RequestBody RegisterRequest request) {

        User user = authService.register(request);

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getStatus());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest request,
            HttpSession session) {
        User user = authService.login(request);
        session.setAttribute("userId", user.getId());
        LoginResponse response = new LoginResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstname(),
                user.getLastname(),
                user.getStatus());
        return ResponseEntity.ok(response);
    }

    // Auth 
    @GetMapping
    public ResponseEntity<Map<String, Object>> SessionAuth(HttpSession session) {
        Map<String, Object> userId = new HashMap<>();
        userId.put("userId", session.getAttribute("userId"));
        return ResponseEntity.ok(userId);
    }

    // logout
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
