package com.github.authorization.exception;

/**
 * @author hongtao
 */
public class PermissionDeniedException extends RuntimeException {
    private int code = 403;

    public PermissionDeniedException(String msg) {
        super(msg);
    }

    public int getCode() {
        return code;
    }
}
