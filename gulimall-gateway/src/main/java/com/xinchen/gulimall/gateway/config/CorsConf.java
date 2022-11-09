package com.xinchen.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * 标注它是一个配置类
 */
@Configuration
public class CorsConf {

    //@Bean加入到容器中
    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        //1.配置跨域
        //允许哪些头进行跨域
        config.addAllowedHeader("*");
        //允许哪些方法进行跨域
        config.addAllowedMethod("*");
        //允许哪些请求来源进行跨域
        config.addAllowedOrigin("http://localhost:8001");
        //允许携带cookie进行跨域
        //When allowCredentials is true, allowedOrigins cannot contain the special value "*"
        //since that cannot be set on the "Access-Control-Allow-Origin" response header
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**",config);
        return new CorsWebFilter(source);
    }
}
