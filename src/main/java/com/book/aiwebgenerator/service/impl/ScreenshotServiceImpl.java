package com.book.aiwebgenerator.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.manager.R2Manager;
import com.book.aiwebgenerator.service.ScreenshotService;
import com.book.aiwebgenerator.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    @Resource
    private R2Manager r2Manager;

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
        String objectKey = generateScreenshotKey(fileName);

        String uploadResult = r2Manager.uploadFile(objectKey, screenshotFile);
        if (StrUtil.isBlank(uploadResult)) {
            log.error("Failed to upload screenshot to R2, key={}", objectKey);
            return null;
        }
        return uploadResult;
    }

    /**
     * Generate object key, no leading slash.
     * Format: screenshots/2026/05/16/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("screenshots/%s/%s", datePath, fileName);
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
