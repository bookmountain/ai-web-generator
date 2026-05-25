package com.book.aiwebgenerator.ai.tools;

import cn.hutool.json.JSONObject;

/**
 * Base tool class.
 * Defines common interface for all tools.
 */
public abstract class BaseTool {

    /**
     * Get the English name of the tool (corresponds to method name).
     *
     * @return Tool English name
     */
    public abstract String getToolName();

    /**
     * Get the Chinese display name of the tool.
     *
     * @return Tool Chinese display name
     */
    public abstract String getDisplayName();

    /**
     * Generate the return value when tool request is made (displayed to user).
     *
     * @return Formatted tool request display content
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[Tool Selected] %s\n\n", getDisplayName());
    }

    /**
     * Generate formatted tool execution result (saved to database).
     *
     * @param arguments Tool execution arguments
     * @return Formatted tool execution result
     */
    public abstract String generateToolExecutedResult(JSONObject arguments);
}
