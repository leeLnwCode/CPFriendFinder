package com.cp.friend.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import com.cp.friend.model.Interest;
import com.cp.friend.repository.InterestRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;

    @InjectMocks
    private InterestService interestService;

    @Test
    void allInterests_withData_returnsRepositoryList() {
        Interest interest = new Interest();
        interest.setName("Music");

        List<Interest> interests = List.of(interest);

        when(interestRepository.findAll())
                .thenReturn(interests);

        List<Interest> result = interestService.allInterests();

        assertSame(interests, result);
    }

    @Test
    void allInterests_withoutData_returnsEmptyList() {
        List<Interest> interests = Collections.emptyList();

        when(interestRepository.findAll())
                .thenReturn(interests);

        List<Interest> result = interestService.allInterests();

        assertSame(interests, result);
    }
}
