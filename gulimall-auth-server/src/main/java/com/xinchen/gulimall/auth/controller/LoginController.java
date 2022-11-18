package com.xinchen.gulimall.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping({"/","/login.html"})
    public String toLogin(){
        return "login";
    }

    @GetMapping("/register.html")
    public String toRegister(){
        return "register";
    }
}
