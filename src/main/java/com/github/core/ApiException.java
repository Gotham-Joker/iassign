package com.github.core;

public class ApiException extends RuntimeException {
    private Integer code;

    public ApiException(Integer code, String msg) {
        super(msg);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
