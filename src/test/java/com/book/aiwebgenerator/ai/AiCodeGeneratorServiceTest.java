package com.book.aiwebgenerator.ai;

import com.book.aiwebgenerator.ai.model.HtmlCodeResult;
import com.book.aiwebgenerator.ai.model.MultiFileCodeResult;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiCodeGeneratorServiceTest {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Test
    void generateHtmlCode() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("it should be not longer than 20 lines");
        Assertions.assertNotNull(result);
    }

    @Test
    void generateMultiFileCode() {
        MultiFileCodeResult result = aiCodeGeneratorService.generateMultiFileCode("it should be not longer than 50 lines");
        Assertions.assertNotNull(result);
    }

    @Test
    void testChatMemory() {
        HtmlCodeResult result = aiCodeGeneratorService.generateHtmlCode("Create a tool website like Programmer Fish, with total code no more than 20 lines");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("Don't generate a website. Tell me what you just made?");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("Create a tool website like Programmer Fish, with total code no more than 20 lines");
        Assertions.assertNotNull(result);
        result = aiCodeGeneratorService.generateHtmlCode("Don't generate a website. Tell me what you just made?");
        Assertions.assertNotNull(result);
    }

}