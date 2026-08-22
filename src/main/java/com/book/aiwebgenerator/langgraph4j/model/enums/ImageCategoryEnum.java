package com.book.aiwebgenerator.langgraph4j.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum ImageCategoryEnum {

    CONTENT("Content image", "CONTENT"),
    LOGO("Logo image", "LOGO"),
    ILLUSTRATION("Illustration image", "ILLUSTRATION"),
    ARCHITECTURE("Architecture image", "ARCHITECTURE");

    private final String text;

    private final String value;

    ImageCategoryEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static ImageCategoryEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ImageCategoryEnum anEnum : ImageCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}