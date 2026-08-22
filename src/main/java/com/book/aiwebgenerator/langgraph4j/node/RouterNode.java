package com.book.aiwebgenerator.langgraph4j.node;

import com.book.aiwebgenerator.ai.AiCodeGenTypeRoutingService;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class RouterNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Executing node: Intelligent Routing");

            CodeGenTypeEnum generationType;
            try {
                // Get AI routing service
                AiCodeGenTypeRoutingService routingService = SpringContextUtil.getBean(AiCodeGenTypeRoutingService.class);
                // Perform intelligent routing based on the original prompt
                generationType = routingService.routeCodeGenType(context.getOriginalPrompt());
                log.info("AI intelligent routing completed, selected type: {} ({})", generationType.getValue(), generationType.getText());
            } catch (Exception e) {
                log.error("AI intelligent routing failed, using default HTML type: {}", e.getMessage());
                generationType = CodeGenTypeEnum.HTML;
            }

            // Update state
            context.setCurrentStep("Intelligent Routing");
            context.setGenerationType(generationType);
            return WorkflowContext.saveContext(context);
        });
    }
}