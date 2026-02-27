package com.book.aiwebgenerator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum UserRoleEnum {

    USER("user", "user"),
    ADMIN("admin", "admin");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        return Arrays.stream(UserRoleEnum.values())
                .filter(anEnum -> anEnum.value.equals(value))
                .findFirst()
                .orElse(null);
    }
}
