package com.book.aiwebgenerator.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    PARAMS_ERROR(40000, "Params Error"),
    NOT_LOGIN_ERROR(40100, "Not Login"),
    NO_AUTH_ERROR(40101, "Unauthorized"),
    NOT_FOUND_ERROR(40400, "Not Found"),
    FORBIDDEN_ERROR(40300, "Forbidden"),
    SYSTEM_ERROR(50000, "System Error"),
    OPERATION_ERROR(50001, "Operation Error"),;

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}

