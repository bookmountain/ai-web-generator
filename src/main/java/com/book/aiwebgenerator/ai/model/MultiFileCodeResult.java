package com.book.aiwebgenerator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Description("Generated HTML, CSS, and JS code result")
@Data
public class MultiFileCodeResult {

    @Description("HTML code")
    private String htmlCode;

    @Description("CSS code")
    private String cssCode;

    @Description("JS code")
    private String jsCode;

    @Description("Description")
    private String description;
}
