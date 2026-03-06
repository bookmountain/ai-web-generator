package com.book.aiwebgenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum CodeGenTypeEnum {

    HTML("Native HTML mode", "html"),
    MULTI_FILE("Native Multiple File mode", "multi_file");

    private final String text;
    private final String value;

    CodeGenTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static CodeGenTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (CodeGenTypeEnum anEnum : CodeGenTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
