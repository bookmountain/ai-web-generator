package com.book.aiwebgenerator.ai.guardrail;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

public class RetryOutputGuardrail implements OutputGuardrail {

    @Override
    public OutputGuardrailResult validate(AiMessage responseFromLLM) {
        String response = responseFromLLM.text();
        // Check if response is empty or too short
        if (response == null || response.trim().isEmpty()) {
            return reprompt("Response content is empty", "Please regenerate the complete content");
        }
        if (response.trim().length() < 10) {
            return reprompt("Response content is too short", "Please provide more detailed content");
        }
        // Check if sensitive or inappropriate content is included
        if (containsSensitiveContent(response)) {
            return reprompt("Contains sensitive information", "Please regenerate the content avoiding sensitive information");
        }
        return success();
    }

    /**
     * Checks whether the content contains sensitive information
     */
    private boolean containsSensitiveContent(String response) {
        String lowerResponse = response.toLowerCase();
        String[] sensitiveWords = {
                "password", "secret", "token",
                "api key", "private key", "certificate", "credential"
        };
        for (String word : sensitiveWords) {
            if (lowerResponse.contains(word)) {
                return true;
            }
        }
        return false;
    }
}