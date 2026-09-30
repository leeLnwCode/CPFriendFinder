package com.cp.friend.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cp.friend.model.Interest;
import com.cp.friend.repository.InterestRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class InterestService {
    
    private final InterestRepository interestRepo;

    @Transactional
    public List<Interest> allInterests() {
        return interestRepo.findAll();
    }

}
