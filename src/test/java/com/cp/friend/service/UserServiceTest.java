package com.cp.friend.service;

import com.cp.friend.dto.request.UpdateProfileRequest;
import com.cp.friend.model.User;
import com.cp.friend.port.StoragePort;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StoragePort storagePort;

    @Mock
    private UserInterests userInterests;

    private UserService userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, storagePort, userInterests);

        userId = UUID.randomUUID();

        user = new User();
        user.setFirstname("Old");
        user.setLastname("Name");
        user.setBio("Old bio");
        user.setDepartment("CS");
        user.setYear((short) 2);
        user.setDateOfBirth(LocalDate.of(2005, 1, 1));
    }

    @Test
    void getProfile_existingUser_returnsUser() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        User result = userService.getProfile(userId);

        assertSame(user, result);
    }

    @Test
    void getProfile_missingUser_returnsNotFound() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getProfile(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getReason());
    }

    @Test
    void updateProfile_updatesProvidedFields() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstname("  New  ");
        request.setLastname("  User  ");
        request.setBio("  New bio  ");
        request.setDepartment("  AI  ");
        request.setYear((short) 3);
        request.setDateOfBirth(LocalDate.of(2004, 5, 10));

        User result = userService.updateProfile(userId, request);

        assertEquals("New", result.getFirstname());
        assertEquals("User", result.getLastname());
        assertEquals("New bio", result.getBio());
        assertEquals("AI", result.getDepartment());
        assertEquals((short) 3, result.getYear());
        assertEquals(
                LocalDate.of(2004, 5, 10),
                result.getDateOfBirth()
        );
    }

    @Test
    void updateProfile_omittedFields_keepOldValues() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstname("New");

        User result = userService.updateProfile(userId, request);

        assertEquals("New", result.getFirstname());
        assertEquals("Name", result.getLastname());
        assertEquals("Old bio", result.getBio());
        assertEquals("CS", result.getDepartment());
    }

    @Test
    void updateProfile_withImage_uploadsAndStoresUrl() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        when(storagePort.uploadBase64("data:image/png;base64,AAAA"))
                .thenReturn("profile/test.png");

        when(storagePort.publicUrl("profile/test.png"))
                .thenReturn("https://storage/profile/test.png");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setImageBase64("data:image/png;base64,AAAA");

        User result = userService.updateProfile(userId, request);

        assertEquals(
                "https://storage/profile/test.png",
                result.getImageUrl()
        );

        verify(storagePort)
                .uploadBase64("data:image/png;base64,AAAA");
    }

    @Test
    void updateProfile_withoutImage_doesNotCallStorage() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();

        userService.updateProfile(userId, request);

        verifyNoInteractions(storagePort);
    }

    @Test
    void updateProfile_blankFirstname_clearsFirstname() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(i -> i.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstname("");

        User result = userService.updateProfile(userId, request);

        assertNull(
                result.getFirstname(),
                "Blank firstname should clear the field"
        );
    }
}
