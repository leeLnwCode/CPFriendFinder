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

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_WithValidData_ShouldCreateUser() {

        // Arrange: เตรียมข้อมูลสมัครสมาชิก
        RegisterRequest request = new RegisterRequest();

        request.setEmail("TEST@KKU.AC.TH");
        request.setPassword("password123");
        request.setFirstname("Test");
        request.setLastname("User");

        // จำลองการทำงานของ Dependencies
        when(userRepository.existsByEmail("test@kku.ac.th"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: เรียกใช้งานฟังก์ชันที่ต้องการทดสอบ
        User result = authService.register(request);

        // Assert: ตรวจสอบผลลัพธ์
        assertNotNull(result);

        assertEquals("test@kku.ac.th", result.getEmail());

        assertEquals("hashed-password", result.getPasswordHash());

        assertEquals(User.Status.ACTIVE, result.getStatus());

        // ตรวจสอบว่ามีการเรียก Repository จริง
        verify(userRepository).save(any(User.class));

        // ตรวจสอบว่ามีการเข้ารหัสรหัสผ่าน
        verify(passwordEncoder).encode("password123");
    }
}