package com.cp.friend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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
        return interestRepo.findByIsActiveTrueOrderByNameAsc();
    }

    @Transactional
    public Interest createInterest(String name) {
        String normalizedName = name.trim();
        if (interestRepo.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Interest already exists");
        }

        Interest interest = new Interest();
        interest.setName(normalizedName);
        return interestRepo.save(interest);
    }

    @Transactional
    public List<Interest> searchInterests(String text) {
        String normalizedText = text.trim();
        if (normalizedText.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is required");
        }
        return interestRepo.findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(normalizedText);
    }

}
