package com.book.aiwebgenerator.langgraph4j;

import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.langgraph4j.model.QualityResult;
import com.book.aiwebgenerator.langgraph4j.node.*;
import com.book.aiwebgenerator.langgraph4j.node.concurrent.*;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.prebuilt.MessagesStateGraph;

import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenSubgraphWorkflow {

    /**
     * Create the content image collection subgraph
     */
    private StateGraph<MessagesState<String>> createContentImageSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("content_collect", ContentImageCollectorNode.create())
                    .addEdge(START, "content_collect")
                    .addEdge("content_collect", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create content image subgraph");
        }
    }

    /**
     * Create the illustration collection subgraph
     */
    private StateGraph<MessagesState<String>> createIllustrationSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("illustration_collect", IllustrationCollectorNode.create())
                    .addEdge(START, "illustration_collect")
                    .addEdge("illustration_collect", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create illustration subgraph");
        }
    }

    /**
     * Create the architecture diagram generation subgraph
     */
    private StateGraph<MessagesState<String>> createDiagramSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("diagram_generate", DiagramCollectorNode.create())
                    .addEdge(START, "diagram_generate")
                    .addEdge("diagram_generate", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create architecture diagram subgraph");
        }
    }

    /**
     * Create the Logo generation subgraph
     */
    private StateGraph<MessagesState<String>> createLogoSubgraph() {
        try {
            return new MessagesStateGraph<String>()
                    .addNode("logo_generate", LogoCollectorNode.create())
                    .addEdge(START, "logo_generate")
                    .addEdge("logo_generate", END);
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create Logo subgraph");
        }
    }

    /**
     * Create the subgraph workflow
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            // Get uncompiled subgraphs (completely sharing state with the parent graph)
            StateGraph<MessagesState<String>> contentImageSubgraph = createContentImageSubgraph();
            StateGraph<MessagesState<String>> illustrationSubgraph = createIllustrationSubgraph();
            StateGraph<MessagesState<String>> diagramSubgraph = createDiagramSubgraph();
            StateGraph<MessagesState<String>> logoSubgraph = createLogoSubgraph();

            return new MessagesStateGraph<String>()
                    // Add regular nodes
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // Add compiled subgraphs as nodes
                    .addNode("content_image_subgraph", contentImageSubgraph)
                    .addNode("illustration_subgraph", illustrationSubgraph)
                    .addNode("diagram_subgraph", diagramSubgraph)
                    .addNode("logo_subgraph", logoSubgraph)

                    // Add image aggregator node
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // Add edges - sequential part
                    .addEdge(START, "image_plan")

                    // Concurrent subgraph branch: dispatch from the planning node to each subgraph
                    .addEdge("image_plan", "content_image_subgraph")
                    .addEdge("image_plan", "illustration_subgraph")
                    .addEdge("image_plan", "diagram_subgraph")
                    .addEdge("image_plan", "logo_subgraph")

                    // Fan-in / convergence: all subgraphs flow into the aggregator
                    .addEdge("content_image_subgraph", "image_aggregator")
                    .addEdge("illustration_subgraph", "image_aggregator")
                    .addEdge("diagram_subgraph", "image_aggregator")
                    .addEdge("logo_subgraph", "image_aggregator")

                    // Continue sequential workflow
                    .addEdge("image_aggregator", "prompt_enhancer")
                    .addEdge("prompt_enhancer", "router")
                    .addEdge("router", "code_generator")
                    .addEdge("code_generator", "code_quality_check")

                    // Quality check conditional edge
                    .addConditionalEdges("code_quality_check",
                            edge_async(this::routeAfterQualityCheck),
                            Map.of(
                                    "build", "project_builder",
                                    "skip_build", END,
                                    "fail", "code_generator"
                            ))
                    .addEdge("project_builder", END)

                    .compile();
        } catch (GraphStateException e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create subgraph workflow");
        }
    }

    /**
     * Routing function: determine the next step based on quality check results
     */
    private String routeAfterQualityCheck(MessagesState<String> state) {
        WorkflowContext context = WorkflowContext.getContext(state);
        QualityResult qualityResult = context.getQualityResult();

        if (qualityResult == null || !qualityResult.getIsValid()) {
            log.error("Code quality check failed, code regeneration required");
            return "fail";
        }

        log.info("Code quality check passed, continuing subsequent steps");
        CodeGenTypeEnum generationType = context.getGenerationType();
        if (generationType == CodeGenTypeEnum.VUE_PROJECT) {
            return "build";
        } else {
            return "skip_build";
        }
    }

    /**
     * Execute the subgraph workflow
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();

        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("Initialization")
                .build();

        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("Subgraph workflow graph:\n{}", graph.content());
        log.info("Starting execution of subgraph code generation workflow");

        WorkflowContext finalContext = null;
        int stepCounter = 1;
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext))) {
            log.info("--- Step {} completed ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("Current step context: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("Subgraph code generation workflow execution completed!");
        return finalContext;
    }
}