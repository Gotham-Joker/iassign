package com.github.oauth2.authorization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.authorization.AccessTokenAuthentication;
import com.github.authorization.AuthenticationContext;
import com.github.authorization.Oauth2Authentication;
import com.github.authorization.UserDetails;
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

    private final SysUserService sysUserService;
    private final AuthorizationServerTokenService accessTokenService;
    private final ObjectMapper objectMapper;

    public OAuthFilter(ObjectMapper objectMapper, AuthorizationServerTokenService accessTokenService, SysUserService sysUserService) {
        this.accessTokenService = accessTokenService;
        this.sysUserService = sysUserService;
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
            Oauth2Authentication oauth2Authentication = accessTokenService.parseAccessToken(accessToken);
            SysUser sysUser = sysUserService.selectById(oauth2Authentication.getId());
            UserDetails userDetails = new UserDetails();
            BeanUtils.copyProperties(sysUser, userDetails);
            AccessTokenAuthentication authentication = new AccessTokenAuthentication(userDetails.id);
            authentication.setDetails(userDetails);
            authentication.setAdmin(sysUser.admin);
            AuthenticationContext.setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } finally {
            AuthenticationContext.clearContext();
        }

    }
}
