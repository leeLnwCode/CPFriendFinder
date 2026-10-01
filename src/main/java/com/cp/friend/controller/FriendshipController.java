package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.response.FriendRemovalResponse;
import com.cp.friend.dto.response.FriendResponse;
import com.cp.friend.service.FriendshipService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping
    public ResponseEntity<List<FriendResponse>> listFriends(HttpSession session) {
        return ResponseEntity.ok(friendshipService.listFriends(currentUserId(session)));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<FriendRemovalResponse> unfriend(
            @PathVariable UUID friendId,
            HttpSession session
    ) {
        return ResponseEntity.ok(friendshipService.unfriend(currentUserId(session), friendId));
    }

    private UUID currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof UUID id) {
            return id;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
