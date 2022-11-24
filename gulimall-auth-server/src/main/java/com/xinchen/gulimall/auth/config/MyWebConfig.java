package com.xinchen.gulimall.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    //重写添加视图器

    /**
     *
     * @param registry 注册中心
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {

        /**
         * 添加视图控制器
         * @GetMapping("/register.html")
         *     public String toRegister(){
         *         return "register";
         *     }
         */
        registry.addViewController("/").setViewName("login");
//        registry.addViewController("/login.html").setViewName("login");
        registry.addViewController("/register.html").setViewName("register");
    }
}
