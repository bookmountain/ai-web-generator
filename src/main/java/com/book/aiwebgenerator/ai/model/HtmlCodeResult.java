package com.book.aiwebgenerator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("Generated HTML code result")
@Data
public class HtmlCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("Description")
    private String description;
}
