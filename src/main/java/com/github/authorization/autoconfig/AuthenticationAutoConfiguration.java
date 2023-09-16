package com.github.authorization.autoconfig;

import com.github.iassign.service.AuthService;
import com.github.authorization.filter.AccessTokenFilter;
import com.github.iassign.service.AccessTokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthenticationAutoConfiguration {

    // 注册accessToken过滤器
    @Bean
    public FilterRegistrationBean accessTokenFilter(AccessTokenService accessTokenService, AuthService authService) {
        FilterRegistrationBean<AccessTokenFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new AccessTokenFilter(accessTokenService, authService));
        bean.addUrlPatterns("/api/*");
        bean.setOrder(10);
        return bean;
    }
}
