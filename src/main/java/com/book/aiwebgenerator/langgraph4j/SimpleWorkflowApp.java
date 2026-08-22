package com.book.aiwebgenerator.langgraph4j;

import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Simplified Website Generation Workflow App - Using MessagesState
 */
@Slf4j
public class SimpleWorkflowApp {

    /**
     * Generic method to create worker nodes
     */
    static AsyncNodeAction<MessagesState<String>> makeNode(String message) {
        return node_async(state -> {
            log.info("Executing node: {}", message);
            return Map.of("messages", message);
        });
    }

    public static void main(String[] args) throws GraphStateException {
        // Create the workflow graph
        CompiledGraph<MessagesState<String>> workflow = new MessagesStateGraph<String>()
                // Add nodes
                .addNode("image_collector", makeNode("Collect image assets"))
                .addNode("prompt_enhancer", makeNode("Enhance prompts"))
                .addNode("router", makeNode("Smart routing selection"))
                .addNode("code_generator", makeNode("Generate website code"))
                .addNode("project_builder", makeNode("Build project"))

                // Add edges
                .addEdge(START, "image_collector")                // START -> Image Collection
                .addEdge("image_collector", "prompt_enhancer")    // Image Collection -> Prompt Enhancement
                .addEdge("prompt_enhancer", "router")             // Prompt Enhancement -> Smart Routing
                .addEdge("router", "code_generator")              // Smart Routing -> Code Generation
                .addEdge("code_generator", "project_builder")     // Code Generation -> Project Building
                .addEdge("project_builder", END)                  // Project Building -> END

                // Compile the workflow
                .compile();

        log.info("Starting workflow execution");

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("Workflow Graph: \n{}", graph.content());

        // Execute the workflow
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(Map.of())) {
            log.info("--- Step {} completed ---", stepCounter);
            log.info("Step output: {}", step);
            stepCounter++;
        }

        log.info("Workflow execution completed!");
    }
}