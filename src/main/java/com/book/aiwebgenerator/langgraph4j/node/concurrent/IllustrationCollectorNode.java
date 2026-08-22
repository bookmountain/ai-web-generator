package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.model.ImageCollectionPlan;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.langgraph4j.tools.UndrawIllustrationTool;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class IllustrationCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> illustrations = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool illustrationTool = SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    log.info("Starting concurrent collection of illustrations, task count: {}", plan.getIllustrationTasks().size());
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        List<ImageResource> images = illustrationTool.searchIllustrations(task.query());
                        if (images != null) {
                            illustrations.addAll(images);
                        }
                    }
                    log.info("Illustration collection completed, collected a total of {} images", illustrations.size());
                }
            } catch (Exception e) {
                log.error("Illustration collection failed: {}", e.getMessage(), e);
            }
            context.setIllustrations(illustrations);
            context.setCurrentStep("Illustration Collection");
            return WorkflowContext.saveContext(context);
        });
    }
}