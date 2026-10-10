package com.cp.friend.service;

import java.util.List;
import java.util.UUID;
import com.cp.friend.dto.request.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.port.StoragePort;
import com.cp.friend.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StoragePort storagePort;

    private AuthService authService;

    @Mock
    private UserInterests userInterestService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                storagePort,
                userInterestService
        );
    }

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@kku.ac.th");
        request.setPassword("12345678");
        request.setFirstname("Jiratchaya");
        request.setLastname("Paocanthuek");
        request.setDateOfBirth(LocalDate.of(2005, 1, 1));
        request.setYear((short) 3);
        request.setDepartment("Computer Science");
        return request;
    }

    private void stubSuccessfulSave() {
        when(passwordEncoder.encode(any(String.class)))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void register_validRequest_savesActiveUser() {
        RegisterRequest request = validRequest();
        stubSuccessfulSave();

        User result = authService.register(request);

        assertNotNull(result);
        assertEquals(User.Status.ACTIVE, result.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_nullRequest_rejected() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(null)
        );

        assertEquals("Registration request is required", ex.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_nullEmail_rejected() {
        RegisterRequest request = validRequest();
        request.setEmail(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is required", ex.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_blankEmail_rejected() {
        RegisterRequest request = validRequest();
        request.setEmail("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email is required", ex.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_nullPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword(null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Password is required", ex.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_blankPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword("   ");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Password is required", ex.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_passwordBelowEightCharacters_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword("1234567");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals(
                "Password must be at least 8 characters",
                ex.getMessage()
        );

        verifyNoInteractions(userRepository, passwordEncoder, storagePort);
    }

    @Test
    void register_duplicateEmail_rejected() {
        RegisterRequest request = validRequest();

        when(userRepository.existsByEmail("test@kku.ac.th"))
                .thenReturn(true);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already exists", ex.getMessage());

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, storagePort);
    }

    @Test
    void register_spacedUppercaseEmail_normalized() {
        RegisterRequest request = validRequest();
        request.setEmail("  TEST@KKU.AC.TH  ");
        stubSuccessfulSave();

        User result = authService.register(request);

        assertEquals("test@kku.ac.th", result.getEmail());
        verify(userRepository)
                .existsByEmail("test@kku.ac.th");
    }

    @Test
    void register_password_isEncodedBeforeSave() {
        RegisterRequest request = validRequest();
        stubSuccessfulSave();

        authService.register(request);

        ArgumentCaptor<User> captor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();

        assertEquals("encoded-password", saved.getPasswordHash());
        verify(passwordEncoder).encode("12345678");
        assertNotEquals("12345678", saved.getPasswordHash());
    }

    @Test
    void register_withImage_uploadsAndStoresPublicUrl() {
        RegisterRequest request = validRequest();
        request.setImageBase64("data:image/png;base64,AAAA");

        stubSuccessfulSave();

        when(storagePort.uploadBase64(request.getImageBase64()))
                .thenReturn("profiles/test.png");

        when(storagePort.publicUrl("profiles/test.png"))
                .thenReturn("https://storage.example/profiles/test.png");

        User result = authService.register(request);

        assertEquals(
                "https://storage.example/profiles/test.png",
                result.getImageUrl()
        );

        verify(storagePort)
                .uploadBase64(request.getImageBase64());

        verify(storagePort)
                .publicUrl("profiles/test.png");
    }

    @Test
    void register_withoutImage_doesNotCallStorage() {
        RegisterRequest request = validRequest();
        request.setImageBase64(null);

        stubSuccessfulSave();

        User result = authService.register(request);

        assertEquals("avatar.png", result.getImageUrl());
        verifyNoInteractions(storagePort);
    }

    @Test
    void register_profileFields_areCopied() {
        RegisterRequest request = validRequest();
        stubSuccessfulSave();

        User result = authService.register(request);

        assertEquals("Jiratchaya", result.getFirstname());
        assertEquals("Paocanthuek", result.getLastname());
        assertEquals(
                LocalDate.of(2005, 1, 1),
                result.getDateOfBirth()
        );
        assertEquals((short) 3, result.getYear());
        assertEquals("Computer Science", result.getDepartment());
    }

    @Test
    void register_withInterests_savesUserInterests() {
        RegisterRequest request = validRequest();

    request.setInterests(
            List.of("Gaming", "Programming")
    );

    UUID userId = UUID.randomUUID();

    User savedUser = mock(User.class);

    when(savedUser.getId())
            .thenReturn(userId);

    when(passwordEncoder.encode(any(String.class)))
            .thenReturn("encoded-password");

    when(userRepository.save(any(User.class)))
            .thenReturn(savedUser);

    authService.register(request);

    verify(userInterestService)
            .replaceInterestsByNames(
                    userId,
                    List.of("Gaming", "Programming")
            );
    }
    @Test
     void register_withoutInterests_doesNotSaveUserInterests() {
        RegisterRequest request = validRequest();

        request.setInterests(null);

        stubSuccessfulSave();

        authService.register(request);

        verifyNoInteractions(userInterestService);
        }

}
