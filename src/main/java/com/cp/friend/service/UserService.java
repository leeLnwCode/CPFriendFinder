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
    private final UserInterests userInterestService;

    @Transactional(readOnly = true)
    public User getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        user.getGalleryPhotos().size();
        return user;
    }

    @Transactional
    public User updateProfile(UUID userId, UpdateProfileRequest request) {

        User user = getProfile(userId);
        if (request.getInterestIds()!=null) userInterestService.replaceInterests(userId,request.getInterestIds());
        if (request.getGalleryPhotos()!=null) {
            if (request.getGalleryPhotos().size()>5) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose at most 5 photos");
            java.util.List<String> photos=new java.util.ArrayList<>();
            for(var photo:request.getGalleryPhotos()) {
                boolean upload=photo.imageBase64()!=null&&!photo.imageBase64().isBlank();
                boolean retained=photo.url()!=null&&!photo.url().isBlank();
                if(upload==retained) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a new photo or an existing photo");
                if(retained) {
                    if(!user.getGalleryPhotos().contains(photo.url())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Unknown existing photo");
                    photos.add(photo.url());
                } else {
                    validatePhoto(photo.imageBase64());
                    String key=storagePort.uploadBase64(photo.imageBase64());
                    photos.add(storagePort.publicUrl(key));
                }
            }
            user.getGalleryPhotos().clear();user.getGalleryPhotos().addAll(photos);
        }

        if (request.getFirstname() != null) {
            user.setFirstname(
                    normalize(request.getFirstname())
            );
        }

        if (request.getLastname() != null) {
            user.setLastname(
                    normalize(request.getLastname())
            );
        }

        if (request.getBio() != null) {
            user.setBio(
                    normalize(request.getBio())
            );
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getYear() != null) {
            user.setYear(request.getYear());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(
                    normalize(request.getDepartment())
            );
        }

        String imageBase64 = request.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            String fileName = storagePort.uploadBase64(imageBase64);
            user.setImageUrl(storagePort.publicUrl(fileName));
        }

        return userRepository.save(user);
    }

    private void validatePhoto(String data) {
        try {
            if(!data.startsWith("data:image/jpeg;base64,")&&!data.startsWith("data:image/png;base64,"))throw new IllegalArgumentException();
            if(data.length()>2800000)throw new IllegalArgumentException();
            byte[] bytes=java.util.Base64.getDecoder().decode(data.substring(data.indexOf(',')+1));
            if(bytes.length>2*1024*1024)throw new IllegalArgumentException();
            try(var input=javax.imageio.ImageIO.createImageInputStream(new java.io.ByteArrayInputStream(bytes))) {
                var readers=javax.imageio.ImageIO.getImageReaders(input);
                if(!readers.hasNext())throw new IllegalArgumentException();
                var reader=readers.next();try{reader.setInput(input);if(reader.getWidth(0)>4096||reader.getHeight(0)>4096||reader.read(0)==null)throw new IllegalArgumentException();}finally{reader.dispose();}
            }
        } catch(Exception error) {throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Photo must be JPEG/PNG, at most 2 MB and 4096 pixels per side");}
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