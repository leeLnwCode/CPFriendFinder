package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.request.CreateInterestRequest;
import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/interest")
@RequiredArgsConstructor 
public class InterestController {
    
    private final InterestService interestService;

    @GetMapping
    public ResponseEntity<List<Interest>> apiInterestAll() {
        return ResponseEntity.ok(interestService.allInterests());
    }

    @PostMapping
    public ResponseEntity<Interest> createInterest(
            @Valid @RequestBody CreateInterestRequest request,
            HttpSession session
    ) {
        requireAuthenticated(session);
        Interest interest = interestService.createInterest(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(interest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterest(@PathVariable UUID id, HttpSession session) {
        requireAuthenticated(session);
        interestService.deleteInterest(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search/{text}")
    public ResponseEntity<List<Interest>> searchInterests(@PathVariable String text) {
        return ResponseEntity.ok(interestService.searchInterests(text));
    }

    private void requireAuthenticated(HttpSession session) {
        if (!(session.getAttribute("userId") instanceof UUID)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
