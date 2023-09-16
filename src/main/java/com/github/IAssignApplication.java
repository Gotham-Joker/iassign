package com.github;

import com.github.core.GlobalIdGenerator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 启动类
 */
@EnableAsync
@EnableScheduling
@SpringBootApplication
@MapperScan(basePackages = "com.**.mapper")
public class IAssignApplication {
    public static void main(String[] args) {
        SpringApplication.run(IAssignApplication.class, args);
    }

    /**
     * 全局ID生成器
     * @return
     */
    @Bean
    public GlobalIdGenerator globalIdGenerator() {
        return new GlobalIdGenerator();
    }

    /**
     * 跨域
     * @return
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(){
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>();
        UrlBasedCorsConfigurationSource conf = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.addAllowedOrigin("*");
        conf.registerCorsConfiguration("/**",config);
        CorsFilter corsFilter = new CorsFilter(conf);
        bean.setFilter(corsFilter);
        bean.addUrlPatterns("/*");
        bean.setOrder(-1);
        return bean;
    }

}
