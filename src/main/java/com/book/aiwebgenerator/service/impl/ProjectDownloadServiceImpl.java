package com.book.aiwebgenerator.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.exception.ThrowUtils;
import com.book.aiwebgenerator.service.ProjectDownloadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownloadServiceImpl implements ProjectDownloadService {

    private static final Set<String> IGNORED_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            "target",
            ".mvn",
            ".idea",
            ".vscode"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );


    private boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // Get the relative path
        Path relativePath = projectRoot.relativize(fullPath);
        // Check each part of the path
        for (Path part : relativePath) {
            String partName = part.toString();
            // Check whether the part is in the ignored names list
            if (IGNORED_NAMES.contains(partName)) {
                return false;
            }
            // Check the file extension
            if (IGNORED_EXTENSIONS.stream().anyMatch(partName::endsWith)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response) {
        // Basic validation
        ThrowUtils.throwIf(StrUtil.isBlank(projectPath), ErrorCode.PARAMS_ERROR, "Project path cannot be blank");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "Download file name cannot be blank");
        File projectDir = new File(projectPath);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.NOT_FOUND_ERROR, "Project directory does not exist");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "The specified path is not a directory");
        log.info("Start packaging and downloading project: {} -> {}.zip", projectPath, downloadFileName);
        // Set HTTP response headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition",
                String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        // Define the file filter
        FileFilter filter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        try {
            // Use Hutool's ZipUtil to compress the filtered directory directly to the response output stream
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, filter, projectDir);
            log.info("Project packaging and download completed: {}", downloadFileName);
        } catch (Exception e) {
            log.error("Exception occurred while packaging project for download", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to package project for download");
        }
    }

}
