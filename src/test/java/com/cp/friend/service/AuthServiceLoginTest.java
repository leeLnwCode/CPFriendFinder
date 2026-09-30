package com.cp.friend.service;

import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.model.User;
import com.cp.friend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Login unit tests mapped to LOGIN-001..LOGIN-011 in the test case workbook. */
@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private LoginRequest validRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@kku.ac.th");
        request.setPassword("password123");
        return request;
    }

    private User activeUser() {
        User user = new User();
        user.setEmail("student@kku.ac.th");
        user.setPasswordHash("encoded-password");
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    @Test // LOGIN-001
    void login_validCredentials_returnsUser() {
        LoginRequest request = validRequest();
        User stored = activeUser();
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("password123", "encoded-password"))
                .thenReturn(true);

        User actual = authService.login(request);

        assertSame(stored, actual);
        verify(passwordEncoder).matches("password123", "encoded-password");
    }

    @Test // LOGIN-002
    void login_nullRequest_rejected() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(null));
        assertEquals("Login request is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // LOGIN-003
    void login_nullEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail(null);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Email is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // LOGIN-004
    void login_blankEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail("  ");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Email is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // LOGIN-005
    void login_nullPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword(null);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Password is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // LOGIN-006
    void login_blankPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword("    ");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Password is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // LOGIN-007
    void login_unknownEmail_rejected() {
        LoginRequest request = validRequest();
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.empty());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", error.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test // LOGIN-008
    void login_blockedAccount_rejected() {
        LoginRequest request = validRequest();
        User stored = activeUser();
        stored.setStatus(User.Status.BLOCKED);
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(stored));
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("User account is not active", error.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @Test // LOGIN-009
    void login_wrongPassword_rejected() {
        LoginRequest request = validRequest();
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(activeUser()));
        when(passwordEncoder.matches("password123", "encoded-password"))
                .thenReturn(false);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.login(request));
        assertEquals("Invalid email or password", error.getMessage());
        verify(passwordEncoder).matches("password123", "encoded-password");
    }

    @Test // LOGIN-010
    void login_spacedUppercaseEmail_normalized() {
        LoginRequest request = validRequest();
        request.setEmail("  STUDENT@KKU.AC.TH ");
        User stored = activeUser();
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("password123", "encoded-password"))
                .thenReturn(true);
        User actual = authService.login(request);
        assertSame(stored, actual);
        verify(userRepository).findByEmail("student@kku.ac.th");
    }

    @Test // LOGIN-011
    void login_blockedAccount_doesNotCheckPassword() {
        LoginRequest request = validRequest();
        User stored = activeUser();
        stored.setStatus(User.Status.BLOCKED);
        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(stored));
        assertThrows(IllegalArgumentException.class, () -> authService.login(request));
        verify(passwordEncoder, never()).matches("password123", "encoded-password");
    }
}
