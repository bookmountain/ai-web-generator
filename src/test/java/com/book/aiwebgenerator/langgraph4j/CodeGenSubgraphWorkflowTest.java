package com.book.aiwebgenerator.langgraph4j;

import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeGenSubgraphWorkflowTest {

    @Test
    void testSubgraphWorkflow() {
        WorkflowContext result = new CodeGenSubgraphWorkflow().executeWorkflow("Build an online learning platform requiring course displays, video playback, and learning progress tracking");
        Assertions.assertNotNull(result);
        System.out.println("Generation Type: " + result.getGenerationType());
        System.out.println("Generated Code Directory: " + result.getGeneratedCodeDir());
        System.out.println("Build Result Directory: " + result.getBuildResultDir());
        System.out.println("Collected Images Count: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }

    @Test
    void testPortfolioWorkflow() {
        WorkflowContext result = new CodeGenSubgraphWorkflow().executeWorkflow("Build a personal portfolio website showcasing project case studies and skill introductions");
        Assertions.assertNotNull(result);
        System.out.println("Generation Type: " + result.getGenerationType());
        System.out.println("Generated Code Directory: " + result.getGeneratedCodeDir());
        System.out.println("Collected Images Count: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }
}