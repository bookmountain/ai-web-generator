package com.book.aiwebgenerator.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class PromptSafetyInputGuardrail implements InputGuardrail {

    // Sensitive word list
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "ignore previous instructions", "ignore above",
            "hack", "繞過", "bypass", "jailbreak"
    );

    // Injection attack patterns
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(?:previous|above|all)\\s+(?:instructions?|commands?|prompts?)"),
            Pattern.compile("(?i)(?:forget|disregard)\\s+(?:everything|all)\\s+(?:above|before)"),
            Pattern.compile("(?i)(?:pretend|act|behave)\\s+(?:as|like)\\s+(?:if|you\\s+are)"),
            Pattern.compile("(?i)system\\s*:\\s*you\\s+are"),
            Pattern.compile("(?i)new\\s+(?:instructions?|commands?|prompts?)\\s*:")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();
        // Check input length
        if (input.length() > 1000) {
            return fatal("Input content is too long; please keep it under 1000 characters");
        }
        // Check if empty
        if (input.trim().isEmpty()) {
            return fatal("Input content cannot be empty");
        }
        // Check sensitive words
        String lowerInput = input.toLowerCase();
        for (String sensitiveWord : SENSITIVE_WORDS) {
            if (lowerInput.contains(sensitiveWord.toLowerCase())) {
                return fatal("Input contains inappropriate content; please modify and try again");
            }
        }
        // Check injection attack patterns
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("Malicious input detected; request rejected");
            }
        }
        return success();
    }
}