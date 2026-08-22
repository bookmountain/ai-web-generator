package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.ai.ImageCollectionPlanService;
import com.book.aiwebgenerator.langgraph4j.model.ImageCollectionPlan;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImagePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            String originalPrompt = context.getOriginalPrompt();
            try {
                // Get the image collection plan service
                ImageCollectionPlanService planService = SpringContextUtil.getBean(ImageCollectionPlanService.class);
                ImageCollectionPlan plan = planService.planImageCollection(originalPrompt);
                log.info("Generated image collection plan, preparing to start concurrent branches");
                // Save the plan to the context
                context.setImageCollectionPlan(plan);
                context.setCurrentStep("Image Planning");
            } catch (Exception e) {
                log.error("Failed to generate image collection plan: {}", e.getMessage(), e);
            }
            return WorkflowContext.saveContext(context);
        });
    }
}