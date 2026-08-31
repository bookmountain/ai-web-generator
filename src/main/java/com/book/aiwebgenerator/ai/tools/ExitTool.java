package com.book.aiwebgenerator.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExitTool extends BaseTool {

    @Override
    public String getToolName() {
        return "exit";
    }

    @Override
    public String getDisplayName() {
        return "Exit Tool Call";
    }

    /**
     * Exit the tool call.
     * Called when the task is complete or no further tool calls are needed.
     *
     * @return Exit confirmation message
     */
    @Tool("Use this tool to exit the operation and prevent loops when the task is completed or no further tool calls are needed")
    public String exit() {
        log.info("AI requested to exit tool execution");
        return "Stop calling tools and output the final result now";
    }

    @Override
    public String generateToolExecutedResult(JSONObject arguments) {
        return "\n\n[Execution Completed]\n\n";
    }
}