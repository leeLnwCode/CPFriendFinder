package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.response.FriendResponse;
import com.cp.friend.service.FriendshipService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController extends SessionController {

    private final FriendshipService friendshipService;

    @GetMapping
    public ResponseEntity<List<FriendResponse>> listFriends(HttpSession session) {
        return ResponseEntity.ok(friendshipService.listFriends(currentUserId(session)));
    }

    // Unfriend
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> unfriend(
            @PathVariable UUID friendId,
            HttpSession session
    ) {
        friendshipService.unfriend(currentUserId(session), friendId);
        return ResponseEntity.noContent().build();
    }
}
