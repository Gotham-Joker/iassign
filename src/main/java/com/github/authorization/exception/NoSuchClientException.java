package com.github.authorization.exception;

public class NoSuchClientException extends OAuth2Exception {
    public NoSuchClientException(String msg) {
        super(404,msg);
    }
}
