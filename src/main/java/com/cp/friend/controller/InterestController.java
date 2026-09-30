package com.cp.friend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cp.friend.model.Interest;
import com.cp.friend.service.InterestService;

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
    

}
