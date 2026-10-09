package com.cp.friend.service;

import com.cp.friend.model.Interest;
import com.cp.friend.repository.InterestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {

    @Mock
    private InterestRepository interestRepository;

    private InterestService interestService;

    @BeforeEach
    void setUp() {
        interestService = new InterestService(interestRepository);
    }

    @Test
    void allInterests_returnsActiveInterestsOrderedByRepository() {
        Interest java = new Interest();
        java.setName("Java");

        Interest web = new Interest();
        web.setName("Web");

        List<Interest> expected = List.of(java, web);

        when(interestRepository.findByIsActiveTrueOrderByNameAsc())
                .thenReturn(expected);

        List<Interest> result = interestService.allInterests();

        assertSame(expected, result);

        verify(interestRepository)
                .findByIsActiveTrueOrderByNameAsc();
    }

    @Test
    void createInterest_validName_trimsAndSaves() {
        when(interestRepository.existsByNameIgnoreCase("Java"))
                .thenReturn(false);

        when(interestRepository.save(any(Interest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Interest result =
                interestService.createInterest("  Java  ");

        assertEquals("Java", result.getName());
        assertTrue(result.isActive());

        ArgumentCaptor<Interest> captor =
                ArgumentCaptor.forClass(Interest.class);

        verify(interestRepository).save(captor.capture());

        assertEquals("Java", captor.getValue().getName());
    }

    @Test
    void createInterest_duplicateName_returnsConflict() {
        when(interestRepository.existsByNameIgnoreCase("Java"))
                .thenReturn(true);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> interestService.createInterest(" Java ")
        );

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Interest already exists", ex.getReason());

        verify(interestRepository, never())
                .save(any());
    }

    @Test
    void searchInterests_trimsSearchText() {
        Interest java = new Interest();
        java.setName("Java");

        when(interestRepository
                .findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
                        "java"
                ))
                .thenReturn(List.of(java));

        List<Interest> result =
                interestService.searchInterests("  java  ");

        assertEquals(1, result.size());
        assertEquals("Java", result.get(0).getName());

        verify(interestRepository)
                .findByNameContainingIgnoreCaseAndIsActiveTrueOrderByNameAsc(
                        "java"
                );
    }

    @Test
    void searchInterests_blankText_returnsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> interestService.searchInterests("   ")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Search text is required", ex.getReason());

        verifyNoInteractions(interestRepository);
    }
}
