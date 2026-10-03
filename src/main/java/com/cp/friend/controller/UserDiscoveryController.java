package com.cp.friend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.response.UserDiscoveryResponse;
import com.cp.friend.service.UserDiscoveryService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserDiscoveryController extends SessionController {

    private final UserDiscoveryService userDiscoveryService;

    // Recommend users by common interests, optional filters
    @GetMapping("/discover")
    public ResponseEntity<List<UserDiscoveryResponse>> discover(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Short year,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                userDiscoveryService.discover(currentUserId(session), department, year)
        );
    }
}
