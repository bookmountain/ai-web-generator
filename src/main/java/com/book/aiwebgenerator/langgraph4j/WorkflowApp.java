package com.book.aiwebgenerator.langgraph4j;

import com.book.aiwebgenerator.langgraph4j.node.*;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
public class WorkflowApp {

    public static void main(String[] args) throws GraphStateException {
        // Create the workflow graph
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // Add nodes - use real worker nodes
                .addNode("image_collector", ImageCollectorNode.create())
                .addNode("prompt_enhancer", PromptEnhancerNode.create())
                .addNode("router", RouterNode.create())
                .addNode("code_generator", CodeGeneratorNode.create())
                .addNode("project_builder", ProjectBuilderNode.create())
                // Add edges
                .addEdge(START, "image_collector")
                .addEdge("image_collector", "prompt_enhancer")
                .addEdge("prompt_enhancer", "router")
                .addEdge("router", "code_generator")
                .addEdge("code_generator", "project_builder")
                .addEdge("project_builder", END)
                // Compile the workflow
                .compile();

        // Initialize WorkflowContext - only set basic information
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt("Create a personal blog website for Book")
                .currentStep("Initialization")
                .build();
        log.info("Initial input: {}", initialContext.getOriginalPrompt());
        log.info("Starting workflow execution");

        // Display workflow graph
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("Workflow Graph:\n{}", graph.content());

        // Execute the workflow
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- Step {} completed ---", stepCounter);
            // Display current state
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                log.info("Current step context: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("Workflow execution completed!");
    }
}