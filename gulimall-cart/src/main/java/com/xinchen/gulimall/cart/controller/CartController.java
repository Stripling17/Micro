package com.xinchen.gulimall.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CartController {

    @GetMapping("/cartList.html")
    public String cartList() {

        return "cartList";
    }

    @GetMapping("/success.html")
    public String success() {

        return "success";
    }
}
