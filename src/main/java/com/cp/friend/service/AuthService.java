package com.cp.friend.service;

import com.cp.friend.config.S3Config;
import com.cp.friend.dto.auth.LoginRequest;
import com.cp.friend.dto.auth.RegisterRequest;
import com.cp.friend.model.User;
import com.cp.friend.repository.UserRepository;
import com.cp.friend.tools.StorageTool;

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
        private final StorageTool storageTool;
        private final S3Config s3config;

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
                        String fileName = storageTool.uploadBase64(request.getImageBase64());
                        user.setImageUrl(fileName);
                }
                user.setFirstname(request.getFirstname());
                user.setLastname(request.getLastname());
                user.setDateOfBirth(request.getDateOfBirth());
                user.setYear(request.getYear());
                user.setDepartment(request.getDepartment());
                user.setImageUrl(s3config.getEndpoint().concat("/storage/").concat(user.getImageUrl()));
                
                return userRepository.save(user);
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
