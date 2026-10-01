package com.cp.friend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cp.friend.config.S3Config;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.tools.StorageTool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StorageTool storageTool;

    @Mock
    private S3Config s3Config;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_validData_savesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@kku.ac.th");
        request.setPassword("password123");
        request.setFirstname("Test");
        request.setLastname("User");

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");
        when(s3Config.getEndpoint())
                .thenReturn("https://storage.test");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertEquals("student@kku.ac.th", result.getEmail());
        assertEquals("hashedPassword", result.getPasswordHash());
        assertEquals(User.Status.ACTIVE, result.getStatus());
        assertEquals("Test", result.getFirstname());
        assertEquals("User", result.getLastname());
        assertEquals(
                "https://storage.test/storage/avatar.png",
                result.getImageUrl());

        verify(userRepository).save(any(User.class));
    }
}
