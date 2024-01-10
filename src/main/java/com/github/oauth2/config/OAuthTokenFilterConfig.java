package com.github.oauth2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.iassign.service.SysUserService;
import com.github.oauth2.authorization.OAuthFilter;
import com.github.oauth2.service.token.AuthorizationServerTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuthTokenFilterConfig {

    // 注册accessToken过滤器
    @Bean
    public FilterRegistrationBean oauthFilter(ObjectMapper objectMapper,
                                              AuthorizationServerTokenService accessTokenService,
                                              SysUserService sysUserService) {
        FilterRegistrationBean<OAuthFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OAuthFilter(objectMapper, accessTokenService, sysUserService));
        bean.addUrlPatterns("/oauth-api/*");
        bean.setOrder(11);
        return bean;
    }
}
