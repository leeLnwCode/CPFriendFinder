package com.cp.friend.tools;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor 
public class StorageTool {

    private final S3Client s3Client;

    @Value("storage")
    private String bucket;


    public String uploadBase64(String base64) {
        String contentType = "image/png";
        if (base64.startsWith("data:")) {
            String[] parts = base64.split(",", 2);
            // data:image/png;base64
            String metadata = parts[0];
            // iVBORw0KGgo...
            base64 = parts[1];
            contentType = metadata.substring(5, metadata.indexOf(";"));
        }
        byte[] fileBytes = Base64.getDecoder().decode(base64);
        String extension = getExtension(contentType);
        String filename = UUID.randomUUID() + "." + extension;
        String key = "uploads/" + filename;

        PutObjectRequest request =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();

        s3Client.putObject(
                request,
                RequestBody.fromBytes(fileBytes)
        );
        return key;
    }

    private String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}