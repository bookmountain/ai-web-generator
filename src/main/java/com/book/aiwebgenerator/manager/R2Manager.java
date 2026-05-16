package com.book.aiwebgenerator.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import com.book.aiwebgenerator.config.R2ClientConfig;
import jakarta.annotation.Resource;

import java.io.File;

@Component
@Slf4j
public class R2Manager {

    @Resource
    private R2ClientConfig r2ClientConfig;

    @Resource
    private S3Client s3Client;

    public PutObjectResponse putObject(String key, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucket())
                .key(key)
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    public String uploadFile(String key, File file) {
        try {
            // Upload file
            PutObjectResponse result = putObject(key, file);
            if (result != null) {
                // Build access URL
                String url = String.format("%s/%s", r2ClientConfig.getEndpoint(), key);
                log.info("File uploaded to R2 successfully: {} -> {}", file.getName(), url);
                return url;
            } else {
                log.error("File upload to R2 failed, return result is empty");
                return null;
            }
        } catch (Exception e) {
            log.error("File upload to R2 failed: {}", e.getMessage(), e);
            return null;
        }
    }
}

