package com.book.aiwebgenerator.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.manager.R2Manager;
import com.book.aiwebgenerator.service.ScreenshotService;
import com.book.aiwebgenerator.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private R2Manager r2Manager;

    @Value("${r2.public-base-url}")
    private String r2PublicBaseUrl;

    @Value("${r2.object-prefix:}")
    private String r2ObjectPrefix;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("Web URL cannot be blank when generating screenshot");
            return null;
        }

        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        if (StrUtil.isBlank(localScreenshotPath)) {
            log.error("Failed to generate screenshot for URL: {}", webUrl);
            return null;
        }

        try {
            return uploadScreenshotToR2(localScreenshotPath);
        } finally {
            cleanupLocalFile(localScreenshotPath);
        }
    }

    /**
     * Upload screenshot to object storage and return public URL for frontend.
     *
     * @param localScreenshotPath local screenshot path
     * @return public URL, null on failure
     */
    private String uploadScreenshotToR2(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }

        File screenshotFile = new File(localScreenshotPath);
        if (!screenshotFile.exists()) {
            log.error("Screenshot file does not exist: {}", localScreenshotPath);
            return null;
        }

        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String objectKey = buildObjectKey(fileName);

        // Upload still uses API credentials/endpoint inside R2Manager
        String uploadResult = r2Manager.uploadFile(objectKey, screenshotFile);
        if (StrUtil.isBlank(uploadResult)) {
            log.error("Failed to upload screenshot to R2, key={}", objectKey);
            return null;
        }

        // Return public URL for frontend display
        return joinUrl(r2PublicBaseUrl, objectKey);
    }

    private String buildObjectKey(String fileName) {
        String screenshotKey = generateScreenshotKey(fileName);
        if (StrUtil.isBlank(r2ObjectPrefix)) {
            return screenshotKey;
        }
        String normalizedPrefix = StrUtil.removePrefix(StrUtil.removeSuffix(r2ObjectPrefix.trim(), "/"), "/");
        return normalizedPrefix + "/" + screenshotKey;
    }

    /**
     * Generate object key, no leading slash.
     * Format: screenshots/2026/05/16/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("screenshots/%s/%s", datePath, fileName);
    }

    private String joinUrl(String baseUrl, String path) {
        String normalizedBase = StrUtil.removeSuffix(baseUrl, "/");
        String normalizedPath = StrUtil.removePrefix(path, "/");
        return normalizedBase + "/" + normalizedPath;
    }

    private void cleanupLocalFile(String localFilePath) {
        File localFile = new File(localFilePath);
        if (localFile.exists()) {
            File parentDir = localFile.getParentFile();
            FileUtil.del(parentDir);
            log.info("Local screenshot file has been cleaned up: {}", localFilePath);
        }
    }
}