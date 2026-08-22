package com.book.aiwebgenerator.langgraph4j.node;

import com.book.aiwebgenerator.constant.AppConstant;
import com.book.aiwebgenerator.core.AiCodeGeneratorFacade;
import com.book.aiwebgenerator.langgraph4j.model.QualityResult;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class CodeGeneratorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Executing node: Code Generation");

            // Use the enhanced prompt as the user message sent to the AI
            String userMessage = buildUserMessage(context);
            CodeGenTypeEnum generationType = context.getGenerationType();
            // Get the AI code generator facade service
            AiCodeGeneratorFacade codeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("Starting code generation, type: {} ({})", generationType.getValue(), generationType.getText());
            // Use a fixed appId for now (to be integrated into the business logic later)
            Long appId = 0L;
            // Call streaming code generation
            Flux<String> codeStream = codeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType, appId);
            // Synchronously wait for the streaming output to complete
            codeStream.blockLast(Duration.ofMinutes(10)); // Wait up to 10 minutes
            // Set the generation directory based on the type
            String generatedCodeDir = String.format("%s/%s_%s", AppConstant.CODE_OUTPUT_ROOT_DIR, generationType.getValue(), appId);
            log.info("AI code generation completed, generation directory: {}", generatedCodeDir);

            // Update state
            context.setCurrentStep("Code Generation");
            context.setGeneratedCodeDir(generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * Construct user message. If there is a quality check failure result, add error fixing information.
     */
    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        // Check if a quality check failure result exists
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // Directly use the error fixing information as the new prompt (serving as a modification)
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    /**
     * Determine whether the quality check failed
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }

    /**
     * Construct the error-fixing prompt
     */
    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## The previously generated code has the following issues that need to be fixed:\n");
        // Add error list
        qualityResult.getErrors().forEach(error ->
                errorInfo.append("- ").append(error).append("\n"));
        // Add repair suggestions (if any)
        if (qualityResult.getSuggestions() != null && !qualityResult.getSuggestions().isEmpty()) {
            errorInfo.append("\n## Suggestions for improvement:\n");
            qualityResult.getSuggestions().forEach(suggestion ->
                    errorInfo.append("- ").append(suggestion).append("\n"));
        }
        errorInfo.append("\nPlease regenerate the code based on the above issues and suggestions, ensuring all mentioned problems are resolved.");
        return errorInfo.toString();
    }
}