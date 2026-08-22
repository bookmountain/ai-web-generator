package com.book.aiwebgenerator.langgraph4j.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ImageCollectionPlan implements Serializable {

    /**
     * List of content image search tasks
     */
    private List<ImageSearchTask> contentImageTasks;

    /**
     * List of illustration image search tasks
     */
    private List<IllustrationTask> illustrationTasks;

    /**
     * List of architecture diagram generation tasks
     */
    private List<DiagramTask> diagramTasks;

    /**
     * List of logo generation tasks
     */
    private List<LogoTask> logoTasks;

    /**
     * Content image search task
     * Corresponds to ImageSearchTool.searchContentImages(String query)
     */
    public record ImageSearchTask(String query) implements Serializable {
    }

    /**
     * Illustration image search task
     * Corresponds to UndrawIllustrationTool.searchIllustrations(String query)
     */
    public record IllustrationTask(String query) implements Serializable {
    }

    /**
     * Architecture diagram generation task
     * Corresponds to MermaidDiagramTool.generateMermaidDiagram(String mermaidCode, String description)
     */
    public record DiagramTask(String mermaidCode, String description) implements Serializable {
    }

    /**
     * Logo generation task
     * Corresponds to LogoGeneratorTool.generateLogos(String description)
     */
    public record LogoTask(String description) implements Serializable {
    }
}