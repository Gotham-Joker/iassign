package com.github.authorization;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;


@Slf4j
public class BearerTokenExtractor {

    public static String extractToken(HttpServletRequest request) {
        // 从http header中获取bearerToken
        String bearerToken = request.getHeader("Authorization");
        if (!StringUtils.hasText(bearerToken)) {
            bearerToken = request.getParameter("token"); // 处理一些特殊情况
        }
        return bearerToken;
    }

}
