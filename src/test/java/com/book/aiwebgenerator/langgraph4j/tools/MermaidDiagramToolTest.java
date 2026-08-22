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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
@EnabledIfSystemProperty(named = "mermaid.integration.enabled", matches = "true")
class MermaidDiagramToolTest {

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Test
    void testGenerateMermaidDiagram() {
        // Test generating a Mermaid architecture diagram
        String mermaidCode = """
                flowchart LR
                    Start([Start]) --> Input[Input Data]
                    Input --> Process[Process Data]
                    Process --> Decision{Is Valid?}
                    Decision -->|Yes| Output[Output Result]
                    Decision -->|No| Error[Error Handling]
                    Output --> End([End])
                    Error --> End
                """;
        String description = "Simple system architecture diagram";
        List<ImageResource> diagrams = mermaidDiagramTool.generateMermaidDiagram(mermaidCode, description);
        assertFalse(diagrams.isEmpty(), "Expected Mermaid SVG to be generated and uploaded to R2");
        ImageResource firstDiagram = diagrams.get(0);
        assertEquals(ImageCategoryEnum.ARCHITECTURE, firstDiagram.getCategory());
        assertEquals(description, firstDiagram.getDescription());
        assertNotNull(firstDiagram.getUrl());
        assertTrue(firstDiagram.getUrl().startsWith("http"));
        try (HttpResponse response = HttpRequest.get(firstDiagram.getUrl()).timeout(15000).execute()) {
            assertTrue(response.isOk(), "Expected uploaded R2 diagram to be publicly accessible");
            assertTrue(response.body().contains("<svg"),
                    "Expected the uploaded R2 object to contain SVG markup");
        }
        System.out.println("Generated Mermaid SVG uploaded to R2: " + firstDiagram.getUrl());
    }
}
