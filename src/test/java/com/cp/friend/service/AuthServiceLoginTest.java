package com.cp.friend.service;

import com.cp.friend.dto.request.LoginRequest;
import com.cp.friend.model.User;
import com.cp.friend.port.StoragePort;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StoragePort storagePort;

    @Mock
    private UserInterests userInterestService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                storagePort,
                userInterestService
        );
    }

    private LoginRequest validRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@kku.ac.th");
        request.setPassword("12345678");
        return request;
    }

    private User activeUser() {
        User user = new User();
        user.setEmail("test@kku.ac.th");
        user.setPasswordHash("encoded-password");
        user.setStatus(User.Status.ACTIVE);
        return user;
    }

    @Test
    void login_validCredentials_returnsUser() {
        LoginRequest request = validRequest();
        User user = activeUser();

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "12345678",
                "encoded-password"
        )).thenReturn(true);

        User result = authService.login(request);

        assertSame(user, result);

        verify(userRepository)
                .findByEmail("test@kku.ac.th");

        verify(passwordEncoder)
                .matches("12345678", "encoded-password");
    }

    @Test
    void login_nullRequest_rejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(null)
        );

        assertEquals(
                "Login request is required",
                ex.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                storagePort
        );
    }

    @Test
    void login_nullEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Email is required", ex.getMessage());

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                storagePort
        );
    }

    @Test
    void login_blankEmail_rejected() {
        LoginRequest request = validRequest();
        request.setEmail("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals("Email is required", ex.getMessage());

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                storagePort
        );
    }

    @Test
    void login_nullPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Password is required",
                ex.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                storagePort
        );
    }

    @Test
    void login_blankPassword_rejected() {
        LoginRequest request = validRequest();
        request.setPassword("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Password is required",
                ex.getMessage()
        );

        verifyNoInteractions(
                userRepository,
                passwordEncoder,
                storagePort
        );
    }

    @Test
    void login_unknownEmail_rejected() {
        LoginRequest request = validRequest();

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                ex.getMessage()
        );

        verifyNoInteractions(passwordEncoder, storagePort);
    }

    @Test
    void login_blockedAccount_rejected() {
        LoginRequest request = validRequest();

        User user = activeUser();
        user.setStatus(User.Status.BLOCKED);

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.of(user));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "User account is not active",
                ex.getMessage()
        );
    }

    @Test
    void login_wrongPassword_rejected() {
        LoginRequest request = validRequest();
        User user = activeUser();

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "12345678",
                "encoded-password"
        )).thenReturn(false);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        assertEquals(
                "Invalid email or password",
                ex.getMessage()
        );
    }

    @Test
    void login_spacedUppercaseEmail_normalized() {
        LoginRequest request = validRequest();
        request.setEmail("  TEST@KKU.AC.TH  ");

        User user = activeUser();

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "12345678",
                "encoded-password"
        )).thenReturn(true);

        User result = authService.login(request);

        assertSame(user, result);

        verify(userRepository)
                .findByEmail("test@kku.ac.th");
    }

    @Test
    void login_blockedAccount_doesNotCheckPassword() {
        LoginRequest request = validRequest();

        User user = activeUser();
        user.setStatus(User.Status.BLOCKED);

        when(userRepository.findByEmail("test@kku.ac.th"))
                .thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> authService.login(request)
        );

        verifyNoInteractions(passwordEncoder, storagePort);
    }
}
