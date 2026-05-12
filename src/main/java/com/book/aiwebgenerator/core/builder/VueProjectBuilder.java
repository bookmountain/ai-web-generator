package com.book.aiwebgenerator.core.builder;

import cn.hutool.core.util.RuntimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class VueProjectBuilder {
    public void buildProjectAsync(String projectPath) {
        Thread.ofVirtual().name("vue-project-bulder-" + System.currentTimeMillis())
                .start(() -> {
                    try {
                        buildProject(projectPath);
                    } catch (Exception e) {
                        log.error("Failed to build Vue project asynchronously, error message: {}", e.getMessage());
                    }
                });
    }

    public boolean buildProject(String projectPath) {
        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            log.error("Project directory does not exist: {}", projectPath);
            return false;
        }

        // Check if package.json exists
        File packageJson = new File(projectDir, "package.json");
        if (!packageJson.exists()) {
            log.error("package.json file does not exist: {}", packageJson.getAbsolutePath());
            return false;
        }
        log.info("Starting to build Vue project: {}", projectPath);
        // Execute npm install
        if (!executeNpmInstall(projectDir)) {
            log.error("npm install execution failed");
            return false;
        }
        // Execute npm run build
        if (!executeNpmBuild(projectDir)) {
            log.error("npm run build execution failed");
            return false;
        }
        // Verify if dist directory was generated
        File distDir = new File(projectDir, "dist");
        if (!distDir.exists()) {
            log.error("Build completed but dist directory was not generated: {}", distDir.getAbsolutePath());
            return false;
        }
        log.info("Vue project built successfully, dist directory: {}", distDir.getAbsolutePath());
        return true;
    }

    /**
     * Execute npm install command
     */
    private boolean executeNpmInstall(File projectDir) {
        log.info("Executing npm install...");
        String command = String.format("%s install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300); // 5 minute timeout
    }

    /**
     * Execute npm run build command
     */
    private boolean executeNpmBuild(File projectDir) {
        log.info("Executing npm run build...");
        String command = String.format("%s run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180);
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    private String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    /**
     * Execute command
     *
     * @param workingDir     working directory
     * @param command        command string
     * @param timeoutSeconds timeout duration in seconds
     * @return whether execution was successful
     */
    private boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("Executing command: {} in directory: {}", command, workingDir.getAbsolutePath());
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // split command into array
            );
            // wait for process to complete with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("Command execution timeout ({}s), forcibly terminating process", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("Command executed successfully: {}", command);
                return true;
            } else {
                log.error("Command execution failed, exit code: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to execute command: {}, error message: {}", command, e.getMessage());
            return false;
        }
    }

}