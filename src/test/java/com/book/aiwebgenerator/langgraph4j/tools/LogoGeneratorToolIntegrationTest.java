package com.book.aiwebgenerator.langgraph4j.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "logo.integration.enabled", matches = "true")
class LogoGeneratorToolIntegrationTest {

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Test
    void generatesSvgWithDeepSeekAndUploadsItToR2() {
        String description = "A minimal blue technology logo with a geometric spark symbol";

        List<ImageResource> logos = logoGeneratorTool.generateLogos(description);

        assertFalse(logos.isEmpty(), "Expected DeepSeek to generate an SVG logo");
        ImageResource logo = logos.getFirst();
        assertEquals(ImageCategoryEnum.LOGO, logo.getCategory());
        assertTrue(logo.getUrl().endsWith("/logo.svg"));

        try (HttpResponse response = HttpRequest.get(logo.getUrl()).timeout(15000).execute()) {
            assertTrue(response.isOk(), "Expected uploaded R2 logo to be publicly accessible");
            assertTrue(response.body().contains("<svg"), "Expected the uploaded R2 object to contain SVG markup");
        }

        System.out.println("Generated SVG logo uploaded to R2: " + logo.getUrl());
    }
}
