package com.book.aiwebgenerator.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import com.book.aiwebgenerator.manager.R2Manager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MermaidDiagramTool {

    @Resource
    private R2Manager r2Manager;

    @Tool("Convert Mermaid code into an architecture diagram image for displaying system structure and technical relationships")
    public List<ImageResource> generateMermaidDiagram(@P("Mermaid diagram code") String mermaidCode,
                                                      @P("Architecture diagram description") String description) {
        if (StrUtil.isBlank(mermaidCode)) {
            return new ArrayList<>();
        }
        File diagramFile = null;
        try {
            // Convert to SVG image
            diagramFile = convertMermaidToSvg(mermaidCode);
            // Upload to Cloudflare R2
            String keyName = String.format("mermaid/%s/%s",
                    RandomUtil.randomString(5), diagramFile.getName());
            String r2Url = r2Manager.uploadFile(keyName, diagramFile);
            if (StrUtil.isNotBlank(r2Url)) {
                return Collections.singletonList(ImageResource.builder()
                        .category(ImageCategoryEnum.ARCHITECTURE)
                        .description(description)
                        .url(r2Url)
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to generate architecture diagram: {}", e.getMessage(), e);
        } finally {
            if (diagramFile != null) {
                FileUtil.del(diagramFile);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Convert Mermaid code into an SVG image
     */
    private File convertMermaidToSvg(String mermaidCode) {
        File tempInputFile = FileUtil.createTempFile("mermaid_input_", ".mmd", true);
        FileUtil.writeUtf8String(mermaidCode, tempInputFile);
        File tempOutputFile = FileUtil.createTempFile("mermaid_output_", ".svg", true);
        boolean succeeded = false;

        try {
            List<String> command = resolveMermaidCommand();
            command.addAll(List.of(
                    "-i", tempInputFile.getAbsolutePath(),
                    "-o", tempOutputFile.getAbsolutePath(),
                    "-b", "transparent",
                    "-q"
            ));

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI timed out after 60 seconds");
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (process.exitValue() != 0 || !tempOutputFile.isFile() || tempOutputFile.length() == 0) {
                String detail = StrUtil.blankToDefault(output, "no command output");
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "Mermaid CLI execution failed: " + detail);
            }
            succeeded = true;
            return tempOutputFile;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "Unable to start Mermaid CLI: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Mermaid CLI execution was interrupted");
        } finally {
            FileUtil.del(tempInputFile);
            if (!succeeded) {
                FileUtil.del(tempOutputFile);
            }
        }
    }

    private List<String> resolveMermaidCommand() {
        Path projectDirectory = Path.of(System.getProperty("user.dir"));
        Path localNode = projectDirectory.resolve(SystemUtil.getOsInfo().isWindows()
                ? "tools/mermaid/node/node.exe"
                : "tools/mermaid/node/node");
        Path localCli = projectDirectory.resolve(
                "tools/mermaid/node_modules/@mermaid-js/mermaid-cli/src/cli.js");

        if (Files.isRegularFile(localNode) && Files.isRegularFile(localCli)) {
            return new ArrayList<>(List.of(localNode.toString(), localCli.toString()));
        }

        String globalCommand = SystemUtil.getOsInfo().isWindows() ? "mmdc.cmd" : "mmdc";
        return new ArrayList<>(List.of(globalCommand));
    }
}
