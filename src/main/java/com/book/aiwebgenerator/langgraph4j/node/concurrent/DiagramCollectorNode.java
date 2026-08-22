package com.book.aiwebgenerator.langgraph4j.node.concurrent;

import com.book.aiwebgenerator.langgraph4j.model.ImageCollectionPlan;
import com.book.aiwebgenerator.langgraph4j.model.ImageResource;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.langgraph4j.tools.MermaidDiagramTool;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class DiagramCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> diagrams = new ArrayList<>();
            try {
                ImageCollectionPlan plan = context.getImageCollectionPlan();
                if (plan != null && plan.getDiagramTasks() != null) {
                    MermaidDiagramTool diagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    log.info("Starting concurrent generation of architecture diagrams, task count: {}", plan.getDiagramTasks().size());
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        List<ImageResource> images = diagramTool.generateMermaidDiagram(
                                task.mermaidCode(), task.description());
                        if (images != null) {
                            diagrams.addAll(images);
                        }
                    }
                    log.info("Architecture diagram generation completed, generated a total of {} images", diagrams.size());
                }
            } catch (Exception e) {
                log.error("Architecture diagram generation failed: {}", e.getMessage(), e);
            }
            context.setDiagrams(diagrams);
            context.setCurrentStep("Architecture Diagram Generation");
            return WorkflowContext.saveContext(context);
        });
    }
}