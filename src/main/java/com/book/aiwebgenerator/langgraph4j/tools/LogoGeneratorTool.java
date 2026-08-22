package com.book.aiwebgenerator.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import com.book.aiwebgenerator.manager.R2Manager;
import com.book.aiwebgenerator.utils.SvgUtils;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class LogoGeneratorTool {

    @Resource
    private ChatModel chatModel;

    @Resource
    private R2Manager r2Manager;

    @Tool("Generate a logo image from a brand description for use on a website")
    public List<ImageResource> generateLogos(
            @P("Detailed logo description, including brand, industry, and visual style") String description) {
        if (StrUtil.isBlank(description)) {
            return new ArrayList<>();
        }

        File logoFile = null;
        try {
            String logoPrompt = """
                    Generate one clean, professional SVG logo mark based on the description below.
                    Return only the SVG markup, starting with <svg and ending with </svg>.
                    The root must include xmlns="http://www.w3.org/2000/svg" and viewBox="0 0 1024 1024".
                    Use a centered composition with simple geometry.
                    Do not include words, letters, numbers, text, signatures, watermarks, scripts,
                    embedded images, external URLs, CSS, animations, or event handlers.
                    Use only these SVG elements: svg, g, path, circle, rect, ellipse, line,
                    polyline, polygon, defs, linearGradient, radialGradient, stop, clipPath,
                    mask, title, and desc.
                    Brand description: %s
                    """.formatted(description.trim());

            String modelResponse = chatModel.chat(logoPrompt);
            String safeSvg = SvgUtils.extractAndSanitizeSvg(modelResponse);
            logoFile = FileUtil.createTempFile("logo_output_", ".svg", true);
            FileUtil.writeUtf8String(safeSvg, logoFile);
            String objectKey = String.format("logos/%s/logo.svg", RandomUtil.randomString(8));
            String logoUrl = r2Manager.uploadFile(objectKey, logoFile);
            if (StrUtil.isBlank(logoUrl)) {
                return new ArrayList<>();
            }

            return Collections.singletonList(ImageResource.builder()
                    .category(ImageCategoryEnum.LOGO)
                    .description(description)
                    .url(logoUrl)
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate logo: {}", e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            if (logoFile != null) {
                FileUtil.del(logoFile);
            }
        }
    }
}
