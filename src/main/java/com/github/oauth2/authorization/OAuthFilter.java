/*
 * MIT License
 *
 * Copyright (c) 2024 Hongtao Liu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.oauth2.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.authorization.*;
import com.github.authorization.exception.OAuth2Exception;
import com.github.core.Result;
import com.github.iassign.entity.SysUser;
import com.github.iassign.service.SysUserService;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

public class OAuthFilter extends OncePerRequestFilter {

    private final AuthorizationServerTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public OAuthFilter(ObjectMapper objectMapper, AuthorizationServerTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String accessToken = request.getParameter("accessToken");
        if (!StringUtils.hasText(accessToken)) {
            accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (!StringUtils.hasText(accessToken)) {
                String result = objectMapper.writeValueAsString(Result.error(401, "oauth token required"));
                response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
                PrintWriter out = response.getWriter();
                out.write(result);
                out.close();
                return;
            }
        }
        try {
            Authentication authentication = accessTokenService.parseAccessToken(accessToken);
            AuthenticationContext.setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (OAuth2Exception e) {
            String result = objectMapper.writeValueAsString(Result.error(e.getCode(), e.getMessage()));
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            PrintWriter out = response.getWriter();
            out.write(result);
            out.close();
        } finally {
            AuthenticationContext.clearContext();
        }

    }
}
