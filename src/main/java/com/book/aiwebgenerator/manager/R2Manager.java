package com.book.aiwebgenerator.manager;

import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.config.R2ClientConfig;
import com.book.aiwebgenerator.utils.UrlUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.File;

@Component
@Slf4j
public class R2Manager {

    @Resource
    private R2ClientConfig r2ClientConfig;

    @Resource
    private S3Client s3Client;

    @Value("${r2.public-base-url}")
    private String publicBaseUrl;

    @Value("${r2.object-prefix:}")
    private String objectPrefix;

    private PutObjectResponse putObject(String objectKey, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(r2ClientConfig.getBucket())
                .key(objectKey)
                .contentType(contentType(file))
                .build();
        return s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    public String uploadFile(String key, File file) {
        if (StrUtil.isBlank(key) || file == null || !file.exists()) {
            log.error("Cannot upload file to R2: key is blank or file does not exist");
            return null;
        }

        try {
            String objectKey = buildObjectKey(key);
            // Upload file
            PutObjectResponse result = putObject(objectKey, file);
            if (result != null) {
                // Return the public URL, not the authenticated S3 API endpoint
                String url = UrlUtils.joinUrl(publicBaseUrl, objectKey);
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

    private String buildObjectKey(String key) {
        String normalizedKey = normalizePath(key);
        String normalizedPrefix = normalizePath(objectPrefix);
        return StrUtil.isBlank(normalizedPrefix)
                ? normalizedKey
                : normalizedPrefix + "/" + normalizedKey;
    }

    private String normalizePath(String path) {
        return StrUtil.blankToDefault(path, "").trim().replaceAll("^/+|/+$", "");
    }

    private String contentType(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
