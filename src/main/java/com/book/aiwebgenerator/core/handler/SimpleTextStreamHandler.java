package com.book.aiwebgenerator.core.handler;

import com.book.aiwebgenerator.model.entity.User;
import com.book.aiwebgenerator.model.enums.ChatHistoryMessageTypeEnum;
import com.book.aiwebgenerator.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Simple text stream handler
 * Handles streaming responses for HTML and MULTI_FILE types
 */

@Slf4j
public class SimpleTextStreamHandler {

    /**
     * Handle traditional streams (HTML, MULTI_FILE)
     * Collect the complete text response directly
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
        StringBuilder aiResponseBuilder = new StringBuilder();
        return originFlux
                .map(chunk -> {
                    // Collect AI response content
                    aiResponseBuilder.append(chunk);
                    return chunk;
                })
                .doOnComplete(() -> {
                    // After the streaming response completes, add the AI message to chat history
                    String aiResponse = aiResponseBuilder.toString();
                    chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                })
                .doOnError(error -> {
                    // If the AI reply fails, also record an error message
                    String errorMessage = "AI reply failed: " + error.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(), loginUser.getId());
                });
    }
}
