package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ImageAggregatorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> allImages = new ArrayList<>();
            log.info("Starting to aggregate concurrently collected images");
            // Aggregate images from various intermediate fields
            if (context.getContentImages() != null) {
                allImages.addAll(context.getContentImages());
            }
            if (context.getIllustrations() != null) {
                allImages.addAll(context.getIllustrations());
            }
            if (context.getDiagrams() != null) {
                allImages.addAll(context.getDiagrams());
            }
            if (context.getLogos() != null) {
                allImages.addAll(context.getLogos());
            }
            log.info("Image aggregation completed, total of {} images", allImages.size());
            // Update the final image list
            context.setImageList(allImages);
            context.setCurrentStep("Image Aggregation");
            return WorkflowContext.saveContext(context);
        });
    }
}