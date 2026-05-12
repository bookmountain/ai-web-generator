package com.book.aiwebgenerator.core.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.book.aiwebgenerator.ai.model.message.*;
import com.book.aiwebgenerator.constant.AppConstant;
import com.book.aiwebgenerator.core.builder.VueProjectBuilder;
import com.book.aiwebgenerator.model.entity.User;
import com.book.aiwebgenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.book.aiwebgenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

/**
 * JSON message stream handler
 * Handles complex streaming responses for VUE_PROJECT type, including tool call information
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    /**
     * Handle TokenStream (VUE_PROJECT)
     * Parse JSON messages and reconstruct them into a complete response format
     *
     * @param originFlux         original stream
     * @param chatHistoryService chat history service
     * @param appId              application ID
     * @param loginUser          logged-in user
     * @return processed stream
     */
    public Flux<String> handle(Flux<String> originFlux,
                               ChatHistoryService chatHistoryService,
                               long appId, User loginUser) {
        // Collect data for generating backend memory format
        StringBuilder chatHistoryStringBuilder = new StringBuilder();
        // Track already seen tool IDs to determine whether this is the first call
        Set<String> seenToolIds = new HashSet<>();
        return originFlux
                .map(chunk -> {
                    // Parse each JSON message chunk
                    return handleJsonMessageChunk(chunk, chatHistoryStringBuilder, seenToolIds);
                })
                .filter(StrUtil::isNotEmpty) // Filter empty strings
                .doOnComplete(() -> {
                    // After the streaming response completes, add the AI message to chat history
                    String aiResponse = chatHistoryStringBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                    String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                    vueProjectBuilder.buildProjectAsync(projectPath);
                })
                .doOnError(error -> {
                    // If the AI reply fails, also record an error message
                    String errorMessage = "AI reply failed: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }

    /**
     * Parse and collect TokenStream data
     */
    private String handleJsonMessageChunk(String chunk, StringBuilder chatHistoryStringBuilder, Set<String> seenToolIds) {
        // Parse JSON
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        StreamMessageTypeEnum typeEnum = StreamMessageTypeEnum.getEnumByValue(streamMessage.getType());
        switch (typeEnum) {
            case AI_RESPONSE -> {
                AiResponseMessage aiMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiMessage.getData();
                // Directly append the response
                chatHistoryStringBuilder.append(data);
                return data;
            }
            case TOOL_REQUEST -> {
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String toolId = toolRequestMessage.getId();
                // Check whether this is the first time seeing this tool ID
                if (toolId != null && !seenToolIds.contains(toolId)) {
                    // First time this tool is called, record the ID and return the full tool info
                    seenToolIds.add(toolId);
                    return "\n\n[Select tool] Write file\n\n";
                } else {
                    // Not the first time seeing this tool ID, return empty directly
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                JSONObject jsonObject = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                String relativeFilePath = jsonObject.getStr("relativeFilePath");
                String suffix = FileUtil.getSuffix(relativeFilePath);
                String content = jsonObject.getStr("content");
                String result = String.format("""
                        [Tool call] Write file %s
                        ```%s
                        %s
                        ```
                        """, relativeFilePath, suffix, content);
                // Output both to the frontend and the content to persist
                String output = String.format("\n\n%s\n\n", result);
                chatHistoryStringBuilder.append(output);
                return output;
            }
            default -> {
                log.error("Unsupported message type: {}", typeEnum);
                return "";
            }
        }
    }
}