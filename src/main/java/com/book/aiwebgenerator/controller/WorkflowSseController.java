package com.book.aiwebgenerator.controller;

import com.book.aiwebgenerator.langgraph4j.CodeGenWorkflow;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

/**
 * Workflow SSE Controller
 * Demonstrates the streaming output functionality of the LangGraph4j workflow
 */
@RestController
@RequestMapping("/workflow")
@Slf4j
public class WorkflowSseController {

    /**
     * Synchronously execute the workflow
     */
    @PostMapping("/execute")
    public WorkflowContext executeWorkflow(@RequestParam String prompt) {
        log.info("Received synchronous workflow execution request: {}", prompt);
        return new CodeGenWorkflow().executeWorkflow(prompt);
    }

    /**
     * Flux stream execution of the workflow
     */
    @GetMapping(value = "/execute-flux", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeWorkflowWithFlux(@RequestParam String prompt) {
        log.info("Received Flux workflow execution request: {}", prompt);
        return new CodeGenWorkflow().executeWorkflowWithFlux(prompt);
    }

    /**
     * SSE stream execution of the workflow
     */
    @GetMapping(value = "/execute-sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter executeWorkflowWithSse(@RequestParam String prompt) {
        log.info("Received SSE workflow execution request: {}", prompt);
        return new CodeGenWorkflow().executeWorkflowWithSse(prompt);
    }
}