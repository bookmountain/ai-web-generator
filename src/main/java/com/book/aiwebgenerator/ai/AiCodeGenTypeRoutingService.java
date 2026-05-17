package com.book.aiwebgenerator.ai;

import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import dev.langchain4j.service.SystemMessage;

/**
 * Intelligent AI routing service for code generation types
 * Uses structured output to return the enum type directly
 */
public interface AiCodeGenTypeRoutingService {

    /**
     * Intelligently select the code generation type based on the user's requirements
     *
     * @param userPrompt the user's requirement description
     * @return the recommended code generation type
     */
    @SystemMessage(fromResource = "prompt/codegen-routing-system-prompt.txt")
    CodeGenTypeEnum routeCodeGenType(String userPrompt);
}