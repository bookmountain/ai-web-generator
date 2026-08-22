package com.book.aiwebgenerator.langgraph4j.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Whether it passed the quality inspection
     */
    private Boolean isValid;

    /**
     * List of errors
     */
    private List<String> errors;

    /**
     * Improvement suggestions
     */
    private List<String> suggestions;
}