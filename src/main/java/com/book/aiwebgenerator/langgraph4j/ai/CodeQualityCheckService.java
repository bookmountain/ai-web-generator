package com.book.aiwebgenerator.langgraph4j.ai;

import com.book.aiwebgenerator.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface CodeQualityCheckService {

    /**
     * Check code quality
     * AI will analyze the code and return the quality inspection result
     */
    @SystemMessage(fromResource = "prompt/code-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String codeContent);
}