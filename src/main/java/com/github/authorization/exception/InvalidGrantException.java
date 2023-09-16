package com.github.authorization.exception;

public class InvalidGrantException extends OAuth2Exception {
    public InvalidGrantException(String msg) {
        super(401, msg);
    }
}
