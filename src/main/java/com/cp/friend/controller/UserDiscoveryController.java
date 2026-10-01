package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.response.UserDiscoveryResponse;
import com.cp.friend.service.UserDiscoveryService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserDiscoveryController {

    private final UserDiscoveryService userDiscoveryService;

    @GetMapping("/discover")
    public ResponseEntity<List<UserDiscoveryResponse>> discover(HttpSession session) {
        return ResponseEntity.ok(userDiscoveryService.discover(currentUserId(session)));
    }

    private UUID currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof UUID id) {
            return id;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
