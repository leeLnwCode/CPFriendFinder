package com.cp.friend.service;

import com.cp.friend.model.Interest;
import com.cp.friend.model.UserInterest;
import com.cp.friend.repository.InterestRepository;
import com.cp.friend.repository.UserInterestRepository;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserInterestServiceTest {

    @Mock
    private UserInterestRepository userInterestRepository;

    @Mock
    private InterestRepository interestRepository;

    @Mock
    private UserRepository userRepository;

    private UserInterestService userInterestService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userInterestService = new UserInterestService(
                userInterestRepository,
                interestRepository,
                userRepository
        );

        userId = UUID.randomUUID();
    }

    private Interest interest(UUID id, String name) {
        Interest interest = new Interest();
        interest.setId(id);
        interest.setName(name);
        interest.setActive(true);
        return interest;
    }

    private UserInterest userInterest(
            UUID userId,
            UUID interestId
    ) {
        UserInterest userInterest = new UserInterest();
        userInterest.setUserId(userId);
        userInterest.setInterestId(interestId);
        return userInterest;
    }

    @Test
    void getInterests_existingUser_returnsActiveInterestsSortedByName() {
        UUID javaId = UUID.randomUUID();
        UUID aiId = UUID.randomUUID();

        Interest java = interest(javaId, "Java");
        Interest ai = interest(aiId, "AI");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(userInterestRepository.findByUserId(userId))
                .thenReturn(List.of(
                        userInterest(userId, javaId),
                        userInterest(userId, aiId)
                ));

        when(interestRepository
                .findByIdInAndIsActiveTrue(anyCollection()))
                .thenReturn(List.of(java, ai));

        List<Interest> result =
                userInterestService.getInterests(userId);

        assertEquals(2, result.size());
        assertEquals("AI", result.get(0).getName());
        assertEquals("Java", result.get(1).getName());
    }

    @Test
    void getInterests_noAssignedInterests_returnsEmptyList() {
        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(userInterestRepository.findByUserId(userId))
                .thenReturn(List.of());

        List<Interest> result =
                userInterestService.getInterests(userId);

        assertTrue(result.isEmpty());

        verifyNoInteractions(interestRepository);
    }

    @Test
    void getInterests_missingUser_returnsNotFound() {
        when(userRepository.existsById(userId))
                .thenReturn(false);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userInterestService.getInterests(userId)
        );

        assertEquals(
                HttpStatus.NOT_FOUND,
                ex.getStatusCode()
        );

        assertEquals(
                "User not found",
                ex.getReason()
        );

        verifyNoInteractions(
                userInterestRepository,
                interestRepository
        );
    }

    @Test
    void replaceInterests_validIds_replacesAndReturnsSortedList() {
        UUID javaId = UUID.randomUUID();
        UUID aiId = UUID.randomUUID();

        Interest java = interest(javaId, "Java");
        Interest ai = interest(aiId, "AI");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(interestRepository
                .findByIdInAndIsActiveTrue(anyCollection()))
                .thenReturn(List.of(java, ai));

        when(userInterestRepository.findByUserId(userId))
                .thenReturn(List.of(
                        userInterest(userId, javaId),
                        userInterest(userId, aiId)
                ));

        List<Interest> result =
                userInterestService.replaceInterests(
                        userId,
                        List.of(javaId, aiId)
                );

        assertEquals(2, result.size());
        assertEquals("AI", result.get(0).getName());
        assertEquals("Java", result.get(1).getName());

        verify(userInterestRepository)
                .deleteAllByUserId(userId);

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Iterable<UserInterest>> captor =
                (ArgumentCaptor) ArgumentCaptor
                        .forClass(Iterable.class);

        verify(userInterestRepository)
                .saveAll(captor.capture());

        List<UserInterest> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertEquals(2, saved.size());

        assertTrue(saved.stream().allMatch(
                item -> userId.equals(item.getUserId())
        ));
    }

    @Test
    void replaceInterests_duplicateIds_savesOnlyOneRelation() {
        UUID javaId = UUID.randomUUID();
        Interest java = interest(javaId, "Java");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(interestRepository
                .findByIdInAndIsActiveTrue(anyCollection()))
                .thenReturn(List.of(java));

        when(userInterestRepository.findByUserId(userId))
                .thenReturn(List.of(
                        userInterest(userId, javaId)
                ));

        userInterestService.replaceInterests(
                userId,
                List.of(javaId, javaId)
        );

        @SuppressWarnings({"rawtypes", "unchecked"})
        ArgumentCaptor<Iterable<UserInterest>> captor =
                (ArgumentCaptor) ArgumentCaptor
                        .forClass(Iterable.class);

        verify(userInterestRepository)
                .saveAll(captor.capture());

        List<UserInterest> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertEquals(1, saved.size());
        assertEquals(javaId, saved.get(0).getInterestId());
    }

    @Test
    void replaceInterests_emptyList_clearsAllInterests() {
        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(userInterestRepository.findByUserId(userId))
                .thenReturn(List.of());

        List<Interest> result =
                userInterestService.replaceInterests(
                        userId,
                        List.of()
                );

        assertTrue(result.isEmpty());

        verify(userInterestRepository)
                .deleteAllByUserId(userId);

        verify(userInterestRepository)
                .saveAll(anyList());

        verifyNoInteractions(interestRepository);
    }

    @Test
    void replaceInterests_missingOrInactiveInterest_returnsBadRequest() {
        UUID validId = UUID.randomUUID();
        UUID invalidId = UUID.randomUUID();

        Interest valid = interest(validId, "Java");

        when(userRepository.existsById(userId))
                .thenReturn(true);

        when(interestRepository
                .findByIdInAndIsActiveTrue(anyCollection()))
                .thenReturn(List.of(valid));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userInterestService.replaceInterests(
                        userId,
                        List.of(validId, invalidId)
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                ex.getStatusCode()
        );

        assertEquals(
                "One or more interests do not exist or are inactive",
                ex.getReason()
        );

        verify(userInterestRepository, never())
                .deleteAllByUserId(any());

        verify(userInterestRepository, never())
                .saveAll(any());
    }
}
