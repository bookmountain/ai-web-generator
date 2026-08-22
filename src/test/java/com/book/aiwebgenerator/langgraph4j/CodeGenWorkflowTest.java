package com.book.aiwebgenerator.langgraph4j;

import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CodeGenWorkflowTest {

    @Test
    void testWorkflowCreation() {
        Assertions.assertNotNull(new CodeGenWorkflow().createWorkflow());
    }

    @Test
    void testTechBlogWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("Build a technical blog website that needs to display programming tutorials and system architecture");
        Assertions.assertNotNull(result);
        System.out.println("Generation type: " + result.getGenerationType());
        System.out.println("Generated code directory: " + result.getGeneratedCodeDir());
        System.out.println("Build result directory: " + result.getBuildResultDir());
    }

    @Test
    void testCorporateWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("Build a corporate website showcasing company image and business introduction");
        Assertions.assertNotNull(result);
        System.out.println("Generation type: " + result.getGenerationType());
        System.out.println("Generated code directory: " + result.getGeneratedCodeDir());
        System.out.println("Build result directory: " + result.getBuildResultDir());
    }

    @Test
    void testVueProjectWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("Build a Vue frontend project including user management and data display features");
        Assertions.assertNotNull(result);
        System.out.println("Generation type: " + result.getGenerationType());
        System.out.println("Generated code directory: " + result.getGeneratedCodeDir());
        System.out.println("Build result directory: " + result.getBuildResultDir());
    }


    @Test
    void testSimpleHtmlWorkflow() {
        WorkflowContext result = new CodeGenWorkflow().executeWorkflow("Build a simple personal homepage");
        Assertions.assertNotNull(result);
        System.out.println("Generation type: " + result.getGenerationType());
        System.out.println("Generated code directory: " + result.getGeneratedCodeDir());
        System.out.println("Build result directory: " + result.getBuildResultDir());
    }
}
