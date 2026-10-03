package com.cp.friend.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.model.Interest;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserInterestService {

    private final UserInterestRepository userInterestRepository;
    private final InterestRepository interestRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Interest> getInterests(UUID userId) {
        ensureUserExists(userId);
        return loadActiveInterests(userId);
    }

    @Transactional
    public List<Interest> replaceInterests(UUID userId, List<UUID> interestIds) {
        ensureUserExists(userId);

        Set<UUID> uniqueIds = new LinkedHashSet<>(interestIds);
        List<Interest> interests = uniqueIds.isEmpty()
                ? List.of()
                : interestRepository.findByIdInAndIsActiveTrue(uniqueIds);

        if (interests.size() != uniqueIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more interests do not exist or are inactive"
            );
        }

        userInterestRepository.deleteAllByUserId(userId);
        List<UserInterest> userInterests = interests.stream()
                .map(interest -> createUserInterest(userId, interest.getId()))
                .toList();
        userInterestRepository.saveAll(userInterests);

        return loadActiveInterests(userId);
    }

    private void ensureUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
    }

    private List<Interest> loadActiveInterests(UUID userId) {
        List<UUID> interestIds = userInterestRepository.findByUserId(userId)
                .stream()
            .map(UserInterest::getInterestId)
                .toList();
        if (interestIds.isEmpty()) {
            return List.of();
        }

        return interestRepository.findByIdInAndIsActiveTrue(interestIds)
            .stream()
            .sorted(Comparator.comparing(Interest::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    private UserInterest createUserInterest(UUID userId, UUID interestId) {
        UserInterest userInterest = new UserInterest();
        userInterest.setUserId(userId);
        userInterest.setInterestId(interestId);
        return userInterest;
    }
}
