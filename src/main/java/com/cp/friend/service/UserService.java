package com.cp.friend.service;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.cp.friend.dto.request.UpdateProfileRequest;
import com.cp.friend.model.User;
import com.cp.friend.port.StoragePort;
import com.cp.friend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    // Dependency Inversion — พึ่ง interface StoragePort ไม่ใช่ S3 SDK โดยตรง
    private final StoragePort storagePort;

    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {

        User user = getProfile(userId);

        String firstname = normalize(request.getFirstname());
        String lastname = normalize(request.getLastname());
        String bio = normalize(request.getBio());
        String department = normalize(request.getDepartment());

        if (firstname != null) {
            user.setFirstname(firstname);
        }
        if (lastname != null) {
            user.setLastname(lastname);
        }
        if (bio != null) {
            user.setBio(bio);
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getYear() != null) {
            user.setYear(request.getYear());
        }
        if (department != null) {
            user.setDepartment(department);
        }

        String imageBase64 = request.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            String fileName = storagePort.uploadBase64(imageBase64);
            user.setImageUrl(storagePort.publicUrl(fileName));
        }

        return userRepository.save(user);
    }

    // trim แล้วถ้าว่างให้เป็น null (ล้างค่า) ไม่งั้นคืนค่าที่ trim แล้ว
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
