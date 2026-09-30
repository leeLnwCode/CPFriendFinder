package com.cp.friend.service;

import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests planned as REG-001..REG-012 in CPFriendFinder_TestCases.xlsx.
 * Pure unit tests: no Spring ApplicationContext and no actual database.
 * Run locally and record ACTUAL results before marking test cases Pass.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@kku.ac.th");
        request.setPassword("password123");
        request.setFirstname("Test");
        request.setLastname("Student");
        return request;
    }

    private void stubSuccessfulSave() {
        when(passwordEncoder.encode(any(String.class))).thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test // REG-001
    void register_validData_savesUser() {
        RegisterRequest request = validRequest();
        stubSuccessfulSave();

        User saved = authService.register(request);

        assertNotNull(saved);
        assertEquals("student@kku.ac.th", saved.getEmail());
        assertEquals("Test", saved.getFirstname());
        assertEquals("Student", saved.getLastname());
        verify(userRepository).existsByEmail("student@kku.ac.th");
        verify(userRepository).save(any(User.class));
    }

    @Test // REG-002
    void register_nullRequest_throwsRequiredRequest() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(null));
        assertEquals("Registration request is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-003
    void register_nullEmail_throwsRequiredEmail() {
        RegisterRequest request = validRequest();
        request.setEmail(null);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Email is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-004
    void register_blankEmail_throwsRequiredEmail() {
        RegisterRequest request = validRequest();
        request.setEmail("   ");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Email is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-005
    void register_nullPassword_throwsRequiredPassword() {
        RegisterRequest request = validRequest();
        request.setPassword(null);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Password is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-006
    void register_blankPassword_throwsRequiredPassword() {
        RegisterRequest request = validRequest();
        request.setPassword("   ");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Password is required", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-007: BVA, minimum password length - 1
    void register_sevenCharacterPassword_rejected() {
        RegisterRequest request = validRequest();
        request.setPassword("1234567");
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Password must be at least 8 characters", error.getMessage());
        verifyNoInteractions(userRepository, passwordEncoder);
    }

    @Test // REG-008: BVA, minimum password length exactly
    void register_eightCharacterPassword_accepted() {
        RegisterRequest request = validRequest();
        request.setPassword("12345678");
        stubSuccessfulSave();
        User saved = authService.register(request);
        assertNotNull(saved);
        verify(passwordEncoder).encode("12345678");
        verify(userRepository).save(any(User.class));
    }

    @Test // REG-009
    void register_duplicateEmail_rejectedWithoutSaving() {
        RegisterRequest request = validRequest();
        when(userRepository.existsByEmail("student@kku.ac.th")).thenReturn(true);
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> authService.register(request));
        assertEquals("Email already exists", error.getMessage());
        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test // REG-010
    void register_spacedUppercaseEmail_normalized() {
        RegisterRequest request = validRequest();
        request.setEmail("  STUDENT@KKU.AC.TH  ");
        stubSuccessfulSave();
        User saved = authService.register(request);
        assertEquals("student@kku.ac.th", saved.getEmail());
        verify(userRepository).existsByEmail("student@kku.ac.th");
    }

    @Test // REG-011
    void register_validData_encodesPasswordAndSetsActive() {
        RegisterRequest request = validRequest();
        stubSuccessfulSave();
        User saved = authService.register(request);
        assertEquals("encoded-password", saved.getPasswordHash());
        assertEquals(User.Status.ACTIVE, saved.getStatus());
        assertNotEquals("password123", saved.getPasswordHash());
        verify(passwordEncoder).encode("password123");
    }

    @Test // REG-012
    void register_invalidEmail_doesNotAccessDependencies() {
        RegisterRequest request = validRequest();
        request.setEmail("");
        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
        verifyNoInteractions(userRepository, passwordEncoder);
    }
}
