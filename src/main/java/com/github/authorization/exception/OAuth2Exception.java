package com.github.authorization.exception;

import com.github.core.ApiException;

public class OAuth2Exception extends ApiException {

    public OAuth2Exception(Integer code, String msg) {
        super(code, msg);
    }


}
