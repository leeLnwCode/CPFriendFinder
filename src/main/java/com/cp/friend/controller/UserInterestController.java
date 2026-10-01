package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.request.UpdateUserInterestsRequest;
import com.cp.friend.model.Interest;
import com.cp.friend.service.UserInterestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users/me/interests")
@RequiredArgsConstructor
public class UserInterestController {

    private final UserInterestService userInterestService;

    @GetMapping
    public ResponseEntity<List<Interest>> getInterests(HttpSession session) {
        return ResponseEntity.ok(userInterestService.getInterests(currentUserId(session)));
    }

    @PutMapping
    public ResponseEntity<List<Interest>> replaceInterests(
            @Valid @RequestBody UpdateUserInterestsRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(
                userInterestService.replaceInterests(currentUserId(session), request.interestIds())
        );
    }

    private UUID currentUserId(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId instanceof UUID id) {
            return id;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
}
