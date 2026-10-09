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
import com.cp.friend.dto.response.InterestResponse;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// คืน InterestResponse (DTO) ผ่าน Mapper — ไม่คืน Interest entity ออกนอก service layer
@RestController
@RequestMapping("/api/interests")
@RequiredArgsConstructor
public class InterestController extends SessionController {

    private final InterestService interestService;
    private final InterestMapper interestMapper;

    // List all interests, optional ?search=
    @GetMapping
    public ResponseEntity<List<InterestResponse>> listInterests(
            @RequestParam(required = false) String search
    ) {
        if (search == null || search.isBlank()) {
            return ResponseEntity.ok(interestMapper.toResponseList(interestService.allInterests()));
        }
        return ResponseEntity.ok(interestMapper.toResponseList(interestService.searchInterests(search)));
    }

    @PostMapping
    public ResponseEntity<InterestResponse> createInterest(
            @Valid @RequestBody CreateInterestRequest request,
            HttpSession session
    ) {
        currentUserId(session);
        Interest interest = interestService.createInterest(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(interestMapper.toResponse(interest));
    }
}
