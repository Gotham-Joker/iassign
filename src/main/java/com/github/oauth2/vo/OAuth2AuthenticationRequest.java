package com.github.oauth2.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 根据 rfc6749 协议,请查看这个章节https://tools.ietf.org/html/rfc6749#section-4.1.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OAuth2AuthenticationRequest {
    private String clientId;
    private String redirectUri;
    private String state;
    private Long timeStamp;
}
