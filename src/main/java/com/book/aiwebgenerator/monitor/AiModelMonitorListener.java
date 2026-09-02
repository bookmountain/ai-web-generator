package com.book.aiwebgenerator.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // Key used to store the request start time
    private static final String REQUEST_START_TIME_KEY = "request_start_time";
    // Key used for monitoring context propagation (since request and response events may run on different threads)
    private static final String MONITOR_CONTEXT_KEY = "monitor_context";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // Record request start time
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        // Retrieve information from the monitoring context
        MonitorContext context = resolveMonitorContext(requestContext.attributes());
        String userId = context.getUserId();
        String appId = context.getAppId();
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, context);
        // Get model name
        String modelName = requestContext.chatRequest().modelName();
        // Record request metric
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "started");
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // Retrieve monitoring info from attributes (saved by the onRequest method)
        Map<Object, Object> attributes = responseContext.attributes();
        // Retrieve information from the monitoring context
        MonitorContext context = resolveMonitorContext(attributes);
        String userId = context.getUserId();
        String appId = context.getAppId();
        // Get model name
        String modelName = responseContext.chatResponse().modelName();
        // Record successful request
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "success");
        // Record response duration
        recordResponseTime(attributes, userId, appId, modelName);
        // Record token usage
        recordTokenUsage(responseContext, userId, appId, modelName);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext context = resolveMonitorContext(attributes);
        String userId = context.getUserId();
        String appId = context.getAppId();
        // Get model name and error type
        String modelName = errorContext.chatRequest().modelName();
        String errorMessage = errorContext.error().getMessage();
        // Record failed request
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        // Record response duration (even for error responses)
        recordResponseTime(attributes, userId, appId, modelName);
    }

    private MonitorContext resolveMonitorContext(Map<Object, Object> attributes) {
        Object savedContext = attributes.get(MONITOR_CONTEXT_KEY);
        if (savedContext instanceof MonitorContext context) {
            return context;
        }
        MonitorContext context = MonitorContextHolder.getContext();
        if (context == null) {
            context = MonitorContext.builder().userId("unknown").appId("unknown").build();
        }
        attributes.put(MONITOR_CONTEXT_KEY, context);
        return context;
    }


    /**
     * Record response duration
     */
    private void recordResponseTime(Map<Object, Object> attributes, String userId, String appId, String modelName) {
        Instant startTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (startTime == null) {
            return;
        }
        Duration responseTime = Duration.between(startTime, Instant.now());
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
    }

    /**
     * Record token usage
     */
    private void recordTokenUsage(ChatModelResponseContext responseContext, String userId, String appId, String modelName) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage != null) {
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "input", tokenUsage.inputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "output", tokenUsage.outputTokenCount());
            aiModelMetricsCollector.recordTokenUsage(userId, appId, modelName, "total", tokenUsage.totalTokenCount());
        }
    }
}
