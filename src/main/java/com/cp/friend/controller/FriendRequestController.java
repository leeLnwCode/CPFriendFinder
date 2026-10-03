package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.CreateFriendRequestRequest;
import com.cp.friend.dto.response.FriendRequestResponse;
import com.cp.friend.service.FriendRequestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/friend-requests")
@RequiredArgsConstructor
public class FriendRequestController extends SessionController {

    private final FriendRequestService friendRequestService;

    @PostMapping
    public ResponseEntity<FriendRequestResponse> send(
            @Valid @RequestBody CreateFriendRequestRequest request,
            HttpSession session
    ) {
        FriendRequestResponse response = friendRequestService.send(currentUserId(session), request.receiverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/incoming")
    public ResponseEntity<List<FriendRequestResponse>> incoming(HttpSession session) {
        return ResponseEntity.ok(friendRequestService.incoming(currentUserId(session)));
    }

    @GetMapping("/outgoing")
    public ResponseEntity<List<FriendRequestResponse>> outgoing(HttpSession session) {
        return ResponseEntity.ok(friendRequestService.outgoing(currentUserId(session)));
    }

    @PostMapping("/{requestId}/accept")
    public ResponseEntity<FriendRequestResponse> accept(@PathVariable UUID requestId, HttpSession session) {
        return ResponseEntity.ok(friendRequestService.accept(currentUserId(session), requestId));
    }

    @PostMapping("/{requestId}/decline")
    public ResponseEntity<FriendRequestResponse> decline(@PathVariable UUID requestId, HttpSession session) {
        return ResponseEntity.ok(friendRequestService.decline(currentUserId(session), requestId));
    }
}
