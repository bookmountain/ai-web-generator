package com.book.aiwebgenerator.langgraph4j.model;

import com.book.aiwebgenerator.langgraph4j.model.enums.ImageCategoryEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResource implements Serializable {


    @Serial
    private static final long serialVersionUID = 1L;
    private ImageCategoryEnum category;
    private String description;
    private String url;
}