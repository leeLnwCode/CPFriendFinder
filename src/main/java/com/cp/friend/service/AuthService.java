package com.cp.friend.service;

import com.cp.friend.dto.request.LoginRequest;
import com.cp.friend.dto.request.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.port.StoragePort;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        // Dependency Inversion — พึ่ง interface StoragePort ไม่ใช่ S3 SDK โดยตรง
        private final StoragePort storagePort;
        private final UserInterestService userInterestService;

        // Register
        @Transactional
        public User register(RegisterRequest request) {

                validateRegistration(request);

                String email = request.getEmail()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                if (userRepository.existsByEmail(email)) {
                        throw new IllegalArgumentException(
                                        "Email already exists");
                }

                User user = new User();

                // Account
                user.setEmail(email);
                user.setPasswordHash(
                                passwordEncoder.encode(request.getPassword()));

                // Status
                user.setStatus(User.Status.ACTIVE);

                // Profile
                if (request.getImageBase64() != null && !request.getImageBase64().isBlank()) {
                        String fileName = storagePort.uploadBase64(request.getImageBase64());
                        user.setImageUrl(storagePort.publicUrl(fileName));
                }
                user.setFirstname(request.getFirstname());
                user.setLastname(request.getLastname());
                user.setDateOfBirth(request.getDateOfBirth());
                user.setYear(request.getYear());
                user.setDepartment(request.getDepartment());

                User savedUser = userRepository.save(user);

                if (
                        request.getInterests() != null &&
                        !request.getInterests().isEmpty()
                ) {
                userInterestService.replaceInterestsByNames(
                        savedUser.getId(),
                        request.getInterests()
                );
                }

                return savedUser;
        }

        private void validateRegistration(RegisterRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Registration request is required");
                }

                if (request.getEmail() == null ||
                                request.getEmail().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Email is required");
                }

                if (request.getPassword() == null ||
                                request.getPassword().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Password is required");
                }

                if (request.getPassword().length() < 8) {
                        throw new IllegalArgumentException(
                                        "Password must be at least 8 characters");
                }
        }

        // Login
        @Transactional
        public User login(LoginRequest request) {

                validateLogin(request);

                String email = request.getEmail()
                                .trim()
                                .toLowerCase(Locale.ROOT);

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Invalid email or password"));

                if (user.getStatus() != User.Status.ACTIVE) {
                        throw new IllegalArgumentException(
                                        "User account is not active");
                }

                if (!passwordEncoder.matches(
                                request.getPassword(),
                                user.getPasswordHash())) {
                        throw new IllegalArgumentException(
                                        "Invalid email or password");
                }

                return user;
        }

        private void validateLogin(LoginRequest request) {

                if (request == null) {
                        throw new IllegalArgumentException(
                                        "Login request is required");
                }

                if (request.getEmail() == null ||
                                request.getEmail().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Email is required");
                }

                if (request.getPassword() == null ||
                                request.getPassword().isBlank()) {

                        throw new IllegalArgumentException(
                                        "Password is required");
                }
        }
}
