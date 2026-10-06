package com.cp.friend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.CreateInterestRequest;
import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController extends SessionController {

    private final InterestService interestService;

    // List all interests, optional ?search=
    @GetMapping
    public ResponseEntity<List<Interest>> listInterests(
            @RequestParam(required = false) String search
    ) {
        if (search == null || search.isBlank()) {
            return ResponseEntity.ok(interestService.allInterests());
        }
        return ResponseEntity.ok(interestService.searchInterests(search));
    }

    @PostMapping
    public ResponseEntity<Interest> createInterest(
            @Valid @RequestBody CreateInterestRequest request,
            HttpSession session
    ) {
        currentUserId(session);
        Interest interest = interestService.createInterest(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(interest);
    }
}
