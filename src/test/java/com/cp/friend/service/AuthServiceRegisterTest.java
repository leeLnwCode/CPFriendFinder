package com.cp.friend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
        RegisterRequest request = validRequest();

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

    @Test
    void register_nullRequest_throwsRequiredRequest() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(null));

        assertEquals(
                "Registration request is required",
                exception.getMessage());
    }

    @Test
    void register_nullEmail_throwsRequiredEmail() {
        RegisterRequest request = validRequest();
        request.setEmail(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void register_blankEmail_throwsRequiredEmail() {
        RegisterRequest request = validRequest();
        request.setEmail("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void register_nullPassword_throwsRequiredPassword() {
        RegisterRequest request = validRequest();
        request.setPassword(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void register_blankPassword_throwsRequiredPassword() {
        RegisterRequest request = validRequest();
        request.setPassword("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void register_sevenCharacterPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword("1234567");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals(
                "Password must be at least 8 characters",
                exception.getMessage());
    }

    @Test
    void register_eightCharacterPassword_accepted() {
        RegisterRequest request = validRequest();
        request.setPassword("12345678");

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(false);
        when(passwordEncoder.encode("12345678"))
                .thenReturn("hashedPassword");
        when(s3Config.getEndpoint())
                .thenReturn("https://storage.test");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertEquals("hashedPassword", result.getPasswordHash());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_duplicateEmail_rejected() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request));

        assertEquals("Email already exists", exception.getMessage());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_emailIsNormalized() {
        RegisterRequest request = validRequest();
        request.setEmail("  STUDENT@KKU.AC.TH  ");

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
        verify(userRepository)
                .existsByEmail("student@kku.ac.th");
    }

    @Test
    void register_passwordIsEncodedAndStatusActive() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encodedPassword");
        when(s3Config.getEndpoint())
                .thenReturn("https://storage.test");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertEquals("encodedPassword", result.getPasswordHash());
        assertEquals(User.Status.ACTIVE, result.getStatus());

        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_imageProvided_uploadsImage() {
        RegisterRequest request = validRequest();
        request.setImageBase64("base64-image");

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");
        when(storageTool.uploadBase64("base64-image"))
                .thenReturn("uploads/profile.png");
        when(s3Config.getEndpoint())
                .thenReturn("https://storage.test");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertEquals(
                "https://storage.test/storage/uploads/profile.png",
                result.getImageUrl());

        verify(storageTool).uploadBase64("base64-image");
    }

    @Test
    void register_blankImage_doesNotUploadImage() {
        RegisterRequest request = validRequest();
        request.setImageBase64("   ");

        when(userRepository.existsByEmail("student@kku.ac.th"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");
        when(s3Config.getEndpoint())
                .thenReturn("https://storage.test");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertEquals(
                "https://storage.test/storage/avatar.png",
                result.getImageUrl());

        verify(storageTool, never()).uploadBase64(anyString());
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@kku.ac.th");
        request.setPassword("password123");
        request.setFirstname("Test");
        request.setLastname("User");
        return request;
    }
}
