package com.book.aiwebgenerator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

@Data
public class AppUpdateRequest implements Serializable {
    private Long id;

    private String appName;

    private static final long serialVersionUID = 1L;
}
