package com.book.aiwebgenerator.ai.tools;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.HashMap;
import java.util.Map;

/**
 * Tool manager
 * Centrally manages all tools and provides lookup by name.
 */
@Slf4j
@Component
public class ToolManager {

    /**
     * Mapping from tool name to tool instance.
     */
    private final Map<String, BaseTool> toolMap = new HashMap<>();

    /**
     * All tools are injected automatically.
     */
    @Resource
    private BaseTool[] tools;

    /**
     * Initialize the tool mapping.
     */
    @PostConstruct
    public void initTools() {
        for (BaseTool tool : tools) {
            toolMap.put(tool.getToolName(), tool);
            log.info("Registered tool: {} -> {}", tool.getToolName(), tool.getDisplayName());
        }
        log.info("ToolManager initialized, {} tools registered", toolMap.size());
    }

    /**
     * Get a tool instance by its name.
     *
     * @param toolName tool's English name
     * @return tool instance
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * Get all registered tools.
     *
     * @return array of tool instances
     */
    public BaseTool[] getAllTools() {
        return tools;
    }
}