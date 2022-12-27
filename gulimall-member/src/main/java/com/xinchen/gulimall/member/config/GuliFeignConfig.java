package com.xinchen.gulimall.member.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Configuration
public class GuliFeignConfig {

    @Bean("requestInterceptor")
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                //System.out.println("拦截器线程..."+Thread.currentThread().getId());
                //1.RequestContextHolder拿到刚进来的这个请求数据
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if(attributes!=null){
                    HttpServletRequest request = attributes.getRequest(); //当前请求
                    if(request != null){
                        //同步请求头数据，Cookie
                        //feign为我们创建的新请求模板；template
                        String cookie = request.getHeader("Cookie");
                        //给新的请求模板中放入原本请求的Header信息（Cookie）
                        template.header("Cookie",cookie);
                    }
//                System.out.println("feign远程之前先进行RequestInterceptor.apply()");
                }
            }
        };
    }

}
