package com.book.aiwebgenerator.langgraph4j.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Image Collection AI Service Interface
 * Uses AI to call tools to collect different types of image resources
 */
public interface ImageCollectionService {

    /**
     * Collects the required image resources based on the user prompt.
     * The AI will independently choose to call the corresponding tools according to the requirements.
     */
    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.txt")
    String collectImages(@UserMessage String userPrompt);
}