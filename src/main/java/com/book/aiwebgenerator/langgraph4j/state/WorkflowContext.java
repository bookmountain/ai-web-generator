package com.book.aiwebgenerator.langgraph4j.state;

import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.model.QualityResult;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Workflow Context - Stores all state information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowContext implements Serializable {

    /**
     * Storage key for WorkflowContext in MessagesState
     */
    public static final String WORKFLOW_CONTEXT_KEY = "workflowContext";
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * Current execution step
     */
    private String currentStep;
    /**
     * User's original input prompt
     */
    private String originalPrompt;
    /**
     * Image resource string
     */
    private String imageListStr;
    /**
     * List of image resources
     */
    private List<ImageResource> imageList;
    /**
     * Enhanced prompt
     */
    private String enhancedPrompt;
    /**
     * Code generation type
     */
    private CodeGenTypeEnum generationType;
    /**
     * Generated code directory
     */
    private String generatedCodeDir;
    /**
     * Successful build directory
     */
    private String buildResultDir;
    /**
     * Quality inspection result
     */
    private QualityResult qualityResult;
    /**
     * Error message
     */
    private String errorMessage;

    // ========== Context Operation Methods ==========

    /**
     * Retrieve WorkflowContext from MessagesState
     */
    public static WorkflowContext getContext(MessagesState<String> state) {
        return (WorkflowContext) state.data().get(WORKFLOW_CONTEXT_KEY);
    }

    /**
     * Save WorkflowContext into MessagesState
     */
    public static Map<String, Object> saveContext(WorkflowContext context) {
        return Map.of(WORKFLOW_CONTEXT_KEY, context);
    }
}