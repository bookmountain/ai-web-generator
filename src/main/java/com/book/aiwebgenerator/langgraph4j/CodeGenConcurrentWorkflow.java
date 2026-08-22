package com.book.aiwebgenerator.langgraph4j;

import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadFactoryBuilder;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

@Slf4j
public class CodeGenConcurrentWorkflow {
    /**
     * Create the concurrent workflow graph
     */
    public CompiledGraph<MessagesState<String>> createWorkflow() {
        try {
            return new MessagesStateGraph<String>()
                    // Add nodes
                    .addNode("image_plan", ImagePlanNode.create())
                    .addNode("prompt_enhancer", PromptEnhancerNode.create())
                    .addNode("router", RouterNode.create())
                    .addNode("code_generator", CodeGeneratorNode.create())
                    .addNode("code_quality_check", CodeQualityCheckNode.create())
                    .addNode("project_builder", ProjectBuilderNode.create())

                    // Add concurrent image collection nodes
                    .addNode("content_image_collector", ContentImageCollectorNode.create())
                    .addNode("illustration_collector", IllustrationCollectorNode.create())
                    .addNode("diagram_collector", DiagramCollectorNode.create())
                    .addNode("logo_collector", LogoCollectorNode.create())
                    .addNode("image_aggregator", ImageAggregatorNode.create())

                    // Add edges
                    .addEdge(START, "image_plan")

                    // Concurrent branch: dispatch from the planning node to each collection node
                    .addEdge("image_plan", "content_image_collector")
                    .addEdge("image_plan", "illustration_collector")
                    .addEdge("image_plan", "diagram_collector")
                    .addEdge("image_plan", "logo_collector")

                    // Fan-in / convergence: all collection nodes flow into the aggregator
                    .addEdge("content_image_collector", "image_aggregator")
                    .addEdge("illustration_collector", "image_aggregator")
                    .addEdge("diagram_collector", "image_aggregator")
                    .addEdge("logo_collector", "image_aggregator")

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
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Failed to create concurrent workflow");
        }
    }

    /**
     * Execute the concurrent workflow
     */
    public WorkflowContext executeWorkflow(String originalPrompt) {
        CompiledGraph<MessagesState<String>> workflow = createWorkflow();
        WorkflowContext initialContext = WorkflowContext.builder()
                .originalPrompt(originalPrompt)
                .currentStep("Initialization")
                .build();
        GraphRepresentation graph = workflow.getGraph(GraphRepresentation.Type.MERMAID);
        log.info("Concurrent workflow graph:\n{}", graph.content());
        log.info("Starting execution of concurrent code generation workflow");
        WorkflowContext finalContext = null;
        int stepCounter = 1;
        // Configure concurrent execution
        ExecutorService pool = ExecutorBuilder.create()
                .setCorePoolSize(10)
                .setMaxPoolSize(20)
                .setWorkQueue(new LinkedBlockingQueue<>(100))
                .setThreadFactory(ThreadFactoryBuilder.create().setNamePrefix("Parallel-Image-Collect").build())
                .build();
        RunnableConfig runnableConfig = RunnableConfig.builder()
                .addParallelNodeExecutor("image_plan", pool)
                .build();
        for (NodeOutput<MessagesState<String>> step : workflow.stream(
                Map.of(WorkflowContext.WORKFLOW_CONTEXT_KEY, initialContext),
                runnableConfig)) {
            log.info("--- Step {} completed ---", stepCounter);
            WorkflowContext currentContext = WorkflowContext.getContext(step.state());
            if (currentContext != null) {
                finalContext = currentContext;
                log.info("Current step context: {}", currentContext);
            }
            stepCounter++;
        }
        log.info("Concurrent code generation workflow execution completed!");
        return finalContext;
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
}