package com.github.authorization.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.iassign.service.AuthService;
import com.github.authorization.Authentication;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.BearerTokenExtractor;
import com.github.authorization.exception.AuthenticationException;
import com.github.authorization.exception.PermissionDeniedException;
import com.github.core.Result;
import com.github.iassign.service.AccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

@Slf4j
public class AccessTokenFilter extends OncePerRequestFilter {
    private final AccessTokenService accessTokenService;
    private final AuthService authService;
    private ObjectMapper objectMapper = new ObjectMapper();

    public AccessTokenFilter(AccessTokenService accessTokenService, AuthService authService) {
        this.accessTokenService = accessTokenService;
        this.authService = authService;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取认证信息，存储到上下文
        try {
            String bearerToken = BearerTokenExtractor.extractToken(request);
            if (StringUtils.hasText(bearerToken)) {
                Authentication authentication = accessTokenService.parse(bearerToken);
                AuthenticationContext.setAuthentication(authentication);
                MDC.put(Result.TRACE_ID, bearerToken);
            }
            String path = request.getRequestURI();

            Result result = authService.decide(path, request.getMethod());
            if (result.code != 0) {
                throw new PermissionDeniedException(result.msg);
            }
            filterChain.doFilter(request, response);
        } catch (PermissionDeniedException pe) {
            String result = objectMapper.writeValueAsString(Result.error(pe.getCode(), pe.getMessage()));
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            PrintWriter out = response.getWriter();
            out.write(result);
            out.close();
        } catch (AuthenticationException e) {
            String result = objectMapper.writeValueAsString(Result.error(401, e.getMessage()));
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE);
            PrintWriter out = response.getWriter();
            out.write(result);
            out.close();
        } finally {
            AuthenticationContext.clearContext();
            MDC.remove(Result.TRACE_ID);
        }
    }


}
