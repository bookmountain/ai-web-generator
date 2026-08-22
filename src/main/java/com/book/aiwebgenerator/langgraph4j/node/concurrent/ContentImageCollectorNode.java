package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.model.ImageCollectionPlan;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.langgraph4j.tools.ImageSearchTool;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ContentImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> contentImages = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    log.info("Starting concurrent collection of content images, task count: {}", plan.getContentImageTasks().size());
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        List<ImageResource> images = imageSearchTool.searchContentImages(task.query());
                        if (images != null) {
                            contentImages.addAll(images);
                        }
                    }
                    log.info("Content image collection completed, collected a total of {} images", contentImages.size());
                }
            } catch (Exception e) {
                log.error("Content image collection failed: {}", e.getMessage(), e);
            }
            // Save the collected images into the intermediate field in the context
            context.setContentImages(contentImages);
            context.setCurrentStep("Content Image Collection");
            return WorkflowContext.saveContext(context);
        });
    }
}