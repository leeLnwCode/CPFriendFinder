package com.cp.friend.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.dto.request.UpdateUserInterestsRequest;
import com.cp.friend.dto.response.InterestResponse;
import com.cp.friend.mapper.InterestMapper;
import com.cp.friend.service.UserInterestService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// คืน InterestResponse (DTO) ผ่าน Mapper — ไม่คืน Interest entity ออกนอก service layer
@RestController
@RequestMapping("/api/users/me/interests")
@RequiredArgsConstructor
public class UserInterestController extends SessionController {

    private final UserInterestService userInterestService;
    private final InterestMapper interestMapper;

    @GetMapping
    public ResponseEntity<List<InterestResponse>> getInterests(HttpSession session) {
        return ResponseEntity.ok(
                interestMapper.toResponseList(userInterestService.getInterests(currentUserId(session))));
    }

    @PutMapping
    public ResponseEntity<List<InterestResponse>> replaceInterests(
            @Valid @RequestBody UpdateUserInterestsRequest request,
            HttpSession session
    ) {
        return ResponseEntity.ok(interestMapper.toResponseList(
                userInterestService.replaceInterests(currentUserId(session), request.interestIds())
        ));
    }
}
