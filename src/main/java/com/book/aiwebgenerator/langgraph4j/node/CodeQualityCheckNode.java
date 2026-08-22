package com.book.aiwebgenerator.langgraph4j.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.langgraph4j.ai.CodeQualityCheckService;
import com.book.aiwebgenerator.langgraph4j.model.QualityResult;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Code Quality Check Node
 */
@Slf4j
public class CodeQualityCheckNode {

    /**
     * File extensions to check
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList(
            ".html", ".htm", ".css", ".js", ".json", ".vue", ".ts", ".jsx", ".tsx"
    );

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Executing node: Code Quality Check");
            String generatedCodeDir = context.getGeneratedCodeDir();
            QualityResult qualityResult;
            try {
                // 1. Read and concatenate code file contents
                String codeContent = readAndConcatenateCodeFiles(generatedCodeDir);
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("No inspectable code files found");
                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(List.of("No inspectable code files found"))
                            .suggestions(List.of("Please ensure that code generation is successful"))
                            .build();
                } else {
                    // 2. Call AI to perform code quality check
                    CodeQualityCheckService qualityCheckService = SpringContextUtil.getBean(CodeQualityCheckService.class);
                    qualityResult = qualityCheckService.checkCodeQuality(codeContent);
                    log.info("Code quality check completed - Passed: {}", qualityResult.getIsValid());
                }
            } catch (Exception e) {
                log.error("Code quality check exception: {}", e.getMessage(), e);
                qualityResult = QualityResult.builder()
                        .isValid(true) // Skip directly to the next step on exception
                        .build();
            }
            // 3. Update state
            context.setCurrentStep("Code Quality Check");
            context.setQualityResult(qualityResult);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * Read and concatenate all code files under the code directory
     */
    private static String readAndConcatenateCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("Code directory does not exist or is not a directory: {}", codeDir);
            return "";
        }
        StringBuilder codeContent = new StringBuilder();
        codeContent.append("# Project File Structure and Code Content\n\n");
        // Use Hutool's walkFiles method to traverse all files
        FileUtil.walkFiles(directory, file -> {
            // Filter criteria: skip hidden files, files in specific directories, and non-code files
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeContent.append("## File: ").append(relativePath).append("\n\n");
                String fileContent = FileUtil.readUtf8String(file);
                codeContent.append(fileContent).append("\n\n");
            }
        });
        return codeContent.toString();
    }

    /**
     * Determine whether this file should be skipped
     */
    private static boolean shouldSkipFile(File file, File rootDir) {
        String relativePath = FileUtil.subPath(rootDir.getAbsolutePath(), file.getAbsolutePath());
        // Skip hidden files
        if (file.getName().startsWith(".")) {
            return true;
        }
        // Skip files in specific directories
        return relativePath.contains("node_modules" + File.separator) ||
                relativePath.contains("dist" + File.separator) ||
                relativePath.contains("target" + File.separator) ||
                relativePath.contains(".git" + File.separator);
    }

    /**
     * Determine whether it is a code file that needs to be checked
     */
    private static boolean isCodeFile(File file) {
        String fileName = file.getName().toLowerCase();
        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}