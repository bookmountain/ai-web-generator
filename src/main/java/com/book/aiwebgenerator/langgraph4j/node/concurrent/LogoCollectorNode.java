package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.model.ImageCollectionPlan;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.langgraph4j.tools.LogoGeneratorTool;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class LogoCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> logos = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getLogoTasks() != null) {
                    LogoGeneratorTool logoTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    log.info("Starting concurrent generation of logos, task count: {}", plan.getLogoTasks().size());
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        List<ImageResource> images = logoTool.generateLogos(task.description());
                        if (images != null) {
                            logos.addAll(images);
                        }
                    }
                    log.info("Logo generation completed, generated a total of {} images", logos.size());
                }
            } catch (Exception e) {
                log.error("Logo generation failed: {}", e.getMessage(), e);
            }
            context.setLogos(logos);
            context.setCurrentStep("Logo Generation");
            return WorkflowContext.saveContext(context);
        });
    }
}