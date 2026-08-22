package com.book.aiwebgenerator.langgraph4j;

import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeGenConcurrentWorkflowTest {

    @Test
    void testConcurrentWorkflow() {
        WorkflowContext result = new CodeGenConcurrentWorkflow().executeWorkflow("Build a tech blog website showcasing programming tutorials and system architecture");
        Assertions.assertNotNull(result);
        System.out.println("Generation Type: " + result.getGenerationType());
        System.out.println("Generated Code Directory: " + result.getGeneratedCodeDir());
        System.out.println("Build Result Directory: " + result.getBuildResultDir());
        System.out.println("Collected Images Count: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }

    @Test
    void testEcommerceWorkflow() {
        WorkflowContext result = new CodeGenConcurrentWorkflow().executeWorkflow("Build an e-commerce website with product displays, a shopping cart, and payment features");
        Assertions.assertNotNull(result);
        System.out.println("Generation Type: " + result.getGenerationType());
        System.out.println("Generated Code Directory: " + result.getGeneratedCodeDir());
        System.out.println("Collected Images Count: " + (result.getImageList() != null ? result.getImageList().size() : 0));
    }
}