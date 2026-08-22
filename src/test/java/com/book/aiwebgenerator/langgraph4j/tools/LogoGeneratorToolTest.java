package com.book.aiwebgenerator.langgraph4j.tools;

import cn.hutool.core.io.FileUtil;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import com.book.aiwebgenerator.manager.R2Manager;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoGeneratorToolTest {

    @Mock
    private ChatModel chatModel;

    @Mock
    private R2Manager r2Manager;

    private LogoGeneratorTool logoGeneratorTool;

    @BeforeEach
    void setUp() {
        logoGeneratorTool = new LogoGeneratorTool();
        ReflectionTestUtils.setField(logoGeneratorTool, "chatModel", chatModel);
        ReflectionTestUtils.setField(logoGeneratorTool, "r2Manager", r2Manager);
    }

    @Test
    void generatesSvgLogoWithDeepSeekAndUploadsItToR2() {
        String generatedSvg = """
                ```svg
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
                    <circle cx="512" cy="512" r="300" fill="#2563eb"/>
                </svg>
                ```
                """;
        AtomicReference<File> uploadedFile = new AtomicReference<>();
        when(chatModel.chat(anyString())).thenReturn(generatedSvg);
        when(r2Manager.uploadFile(anyString(), any(File.class))).thenAnswer(invocation -> {
            File file = invocation.getArgument(1);
            uploadedFile.set(file);
            assertTrue(file.exists());
            assertTrue(FileUtil.readUtf8String(file).startsWith("<svg"));
            return "https://cdn.example.com/logos/abc/logo.svg";
        });

        List<ImageResource> logos = logoGeneratorTool.generateLogos("Minimal blue technology logo");

        assertEquals(1, logos.size());
        assertEquals(ImageCategoryEnum.LOGO, logos.get(0).getCategory());
        assertEquals("Minimal blue technology logo", logos.get(0).getDescription());
        assertEquals("https://cdn.example.com/logos/abc/logo.svg", logos.get(0).getUrl());
        assertFalse(uploadedFile.get().exists());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatModel).chat(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("Do not include words"));

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(r2Manager).uploadFile(keyCaptor.capture(), any(File.class));
        assertTrue(keyCaptor.getValue().matches("logos/[a-zA-Z0-9]{8}/logo\\.svg"));
    }
}
