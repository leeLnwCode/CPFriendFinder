package com.cp.friend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.cp.friend.config.S3Config;
import com.cp.friend.dto.auth.LoginRequest;
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
class AuthServiceLoginTest {

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
    void login_validCredentials_returnsUser() {
        LoginRequest request = validRequest();
        User user = activeUser();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "storedHash"))
                .thenReturn(true);

        User result = authService.login(request);

        assertSame(user, result);
    }

    @Test
    void login_nullRequest_rejected() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(null));

        assertEquals(
                "Login request is required",
                exception.getMessage());
    }

    @Test
    void login_nullEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void login_blankEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Email is required", exception.getMessage());
    }

    @Test
    void login_nullPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword(null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void login_blankPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword("   ");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Password is required", exception.getMessage());
    }

    @Test
    void login_unknownEmail_rejected() {
        LoginRequest request = validRequest();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals(
                "Invalid email or password",
                exception.getMessage());

        verify(passwordEncoder, never())
                .matches("password123", "storedHash");
    }

    @Test
    void login_blockedAccount_rejected() {
        LoginRequest request = validRequest();
        User user = blockedUser();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals(
                "User account is not active",
                exception.getMessage());
    }

    @Test
    void login_wrongPassword_rejected() {
        LoginRequest request = validRequest();
        User user = activeUser();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "storedHash"))
                .thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals(
                "Invalid email or password",
                exception.getMessage());
    }

    @Test
    void login_spacedUppercaseEmail_normalized() {
        LoginRequest request = validRequest();
        request.setEmail("  STUDENT@KKU.AC.TH  ");

        User user = activeUser();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "storedHash"))
                .thenReturn(true);

        User result = authService.login(request);

        assertSame(user, result);

        verify(userRepository)
                .findByEmail("student@kku.ac.th");
    }

    @Test
    void login_blockedAccount_doesNotCheckPassword() {
        LoginRequest request = validRequest();
        User user = blockedUser();

        when(userRepository.findByEmail("student@kku.ac.th"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request));

        verify(passwordEncoder, never())
                .matches("password123", "storedHash");
    }

    private LoginRequest validRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@kku.ac.th");
        request.setPassword("password123");
        return request;
    }

    private User activeUser() {
        User user = new User();
        user.setEmail("student@kku.ac.th");
        user.setPasswordHash("storedHash");
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    private User blockedUser() {
        User user = activeUser();
        user.setStatus(User.Status.BLOCKED);
        return user;
    }
}
