package com.book.aiwebgenerator.ai.tools;

import com.book.aiwebgenerator.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

@Slf4j
public class FileWriteTool {

    @Tool("Write file to designated path")
    public String writeFile(@P("Relative path of the file") String relativeFilePath,
                            @P("Content of the file") String content,
                            @ToolMemoryId Long appId) {
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                // Handle relative paths by creating a project directory based on appId
                String projectDirName = "vue_project_" + appId;
                Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, projectDirName);
                path = projectRoot.resolve(relativeFilePath);
            }
            // Create parent directory if it doesn't exist
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // Write file content
            Files.write(path, content.getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("File written successfully: {}", path.toAbsolutePath());
            // Note: return relative path only, do not return absolute path to the user
            return "File written successfully: " + relativeFilePath;
        } catch (IOException e) {
            String errorMessage = "File write failed: " + relativeFilePath + ", error: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }

    }
}
